package internal

import (
	"encoding/csv"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"maps"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"slices"
	"sort"
	"strconv"
	"strings"
	"time"
)

func ReadTreeFromFile(name string) (Tree, error) {
	treeBytes, err := ReadTreeRaw(name)
	if err != nil {
		return nil, fmt.Errorf("error reading file '%v': %v", name, err)
	}

	var tree Tree
	err = json.Unmarshal(treeBytes, &tree)
	if err != nil {
		return nil, fmt.Errorf("error unmarshalling tree in file '%v': %v", name, err)
	}

	return tree, nil
}

var filenameWithDateRegex = regexp.MustCompile(`^oncotree_(\d{4})_(\d{2})_(\d{2})(?:\.(json|txt))?$`)

type DatedFile struct {
	Name   string
	Date   time.Time
	HasTSV bool
}

func GetSortedTreeFilesWithDate() ([]DatedFile, error) {
	treeFiles, err := os.ReadDir(TREE_FILES_PATH)
	if err != nil {
		return nil, fmt.Errorf("Error reading '%v' directory: %v", TREE_FILES_PATH, err)
	}

	tsvFiles, err := os.ReadDir(TSV_FILES_PATH)
	if err != nil {
		return nil, fmt.Errorf("Error reading '%v' directory: %v", TSV_FILES_PATH, err)
	}

	filesWithDate := make([]DatedFile, 0)
	for _, file := range tsvFiles {
		if !file.IsDir() {
			date, err := GetDateFromFilename(file.Name())
			if err == nil {
				filesWithDate = append(filesWithDate, DatedFile{Name: file.Name(), Date: date, HasTSV: true})
			}
		}
	}
	for _, file := range treeFiles {
		if !file.IsDir() {
			date, err := GetDateFromFilename(file.Name())
			alreadyContainsDate := false
			for _, file := range filesWithDate {
				if file.Date.Equal(date) {
					alreadyContainsDate = true
					break
				}
			}
			if err == nil && !alreadyContainsDate {
				filesWithDate = append(filesWithDate, DatedFile{Name: file.Name(), Date: date, HasTSV: false})
			}
		}
	}
	sort.Slice(filesWithDate, func(i, j int) bool {
		return filesWithDate[i].Date.Before(filesWithDate[j].Date)
	})

	return filesWithDate, nil
}

func GetDateFromFilename(name string) (time.Time, error) {
	matches := filenameWithDateRegex.FindStringSubmatch(name)
	if matches == nil {
		return time.Time{}, fmt.Errorf("Error: file with name '%v' is not of format 'oncotree_YYYY_MM_DD'", name)
	}

	year, err := strconv.Atoi(matches[1])
	if err != nil {
		return time.Time{}, fmt.Errorf("Error converting year to int for '%v'", name)
	}

	month, err := strconv.Atoi(matches[2])
	if err != nil {
		return time.Time{}, fmt.Errorf("Error converting month to int for '%v'", name)
	}

	day, err := strconv.Atoi(matches[3])
	if err != nil {
		return time.Time{}, fmt.Errorf("Error converting day to int for '%v'", name)
	}

	date := time.Date(year, time.Month(month), day, 0, 0, 0, 0, time.UTC)

	return date, nil
}

func GetCodes(filename string) (map[string]struct{}, error) {
	codes := make(map[string]struct{})

	if strings.HasSuffix(filename, ".json") {
		tree, err := ReadTreeFromFile(filename)
		if err != nil {
			return nil, err
		}

		err = tree.BFS(func(node *TreeNode, _ uint) {
			codes[node.Code] = struct{}{}
		})
		if err != nil {
			return nil, err
		}
		return codes, nil
	} else {
		file, err := os.Open(filepath.Join(TSV_FILES_PATH, filename))
		if err != nil {
			return nil, fmt.Errorf("error reading file '%v': %v", file.Name, err)
		}

		reader := csv.NewReader(file)
		reader.Comma = '\t'

		headerRow, err := reader.Read()
		if err == io.EOF {
			return nil, errors.New("missing header row")
		} else if err != nil {
			return nil, fmt.Errorf("error parsing header row: %s", err)
		}

		codeIndex := slices.Index(headerRow, CODE_HEADER)
		if codeIndex == -1 {
			return nil, fmt.Errorf("header row missing '%v' column", CODE_HEADER)
		}

		rowNumber := 1
		for {
			row, err := reader.Read()
			if err == io.EOF {
				break
			} else if err != nil {
				return nil, fmt.Errorf("error parsing row %v", rowNumber)
			}
			codes[row[codeIndex]] = struct{}{}
		}
	}

	return codes, nil
}

type MappingFile struct {
	OldTree string
	NewTree string
}

func (file *MappingFile) GetName() string {
	return file.OldTree + "_to_" + file.NewTree + ".txt"
}

func GetSortedMappingFilesWithDate() ([]MappingFile, error) {
	mappingFileDirEntries, err := os.ReadDir(MAPPING_FILES_PATH)
	if err != nil {
		return nil, fmt.Errorf("error reading '%v' directory: %v", MAPPING_FILES_PATH, err)
	}

	mappingFiles := []MappingFile{}
	for _, file := range mappingFileDirEntries {
		if file.Name() == ".gitkeep" {
			continue
		}

		pieces := strings.Split(strings.Replace(file.Name(), ".txt", "", 1), "_to_")
		mappingFiles = append(mappingFiles, MappingFile{OldTree: pieces[0], NewTree: pieces[1]})
	}

	sort.Slice(mappingFiles, func(i, j int) bool {
		// Don't check error: mapping files have been validated at this point
		startDate1, _ := GetDateFromFilename(mappingFiles[i].OldTree)
		startDate2, _ := GetDateFromFilename(mappingFiles[j].OldTree)

		return startDate1.Before(startDate2)
	})
	return mappingFiles, nil
}

func GetTreeFilepath(name string) string {
	return filepath.Join(TREE_FILES_PATH, name)
}

func (file *DatedFile) GetDatedFilenameWithoutExtension() string {
	name := strings.Replace(file.Name, ".txt", "", 1)
	return strings.Replace(name, ".json", "", 1)
}

func fetchDevTreeIfChanged(devTreePath string) error {
	cacheDir := filepath.Join(TREE_FILES_PATH, "..", "cache")
	if err := os.MkdirAll(cacheDir, 0755); err != nil {
		return fmt.Errorf("failed to create cache dir: %w", err)
	}

	base := filepath.Base(devTreePath)
	tmpPath := filepath.Join(cacheDir, base+".tmp")
	etagPath := filepath.Join(cacheDir, base+".etag")

	req, err := http.NewRequest(http.MethodGet, DEV_TREE_GITHUB_RAW_URL, nil)
	if err != nil {
		return err
	}

	if etag, err := os.ReadFile(etagPath); err == nil {
		req.Header.Set("If-None-Match", string(etag))
	}

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	switch resp.StatusCode {
	case http.StatusNotModified:
		return nil

	case http.StatusOK:
		out, err := os.Create(tmpPath)
		if err != nil {
			return err
		}
		defer out.Close()

		if _, err := io.Copy(out, resp.Body); err != nil {
			return err
		}

		if err := os.Rename(tmpPath, devTreePath); err != nil {
			return err
		}

		if etag := resp.Header.Get("ETag"); etag != "" {
			if err := os.WriteFile(etagPath, []byte(etag), 0644); err != nil {
				return fmt.Errorf("failed to write etag: %w", err)
			}
		}

		return nil

	default:
		return fmt.Errorf("unexpected status from GitHub: %s", resp.Status)
	}
}

func ReadTreeRaw(name string) ([]byte, error) {
	appEnv := os.Getenv("APP_ENV")
	if name == DEV_TREE_IDENTIFIER+".json" && appEnv == "production" {
		devTreePath := filepath.Join(TREE_FILES_PATH, name)
		if err := fetchDevTreeIfChanged(devTreePath); err != nil {
			return nil, err
		}
	}

	return os.ReadFile(GetTreeFilepath(name))
}

func ValidateMappingDir() error {
	mappingFiles, err := os.ReadDir(MAPPING_FILES_PATH)
	if err != nil {
		return fmt.Errorf("Error reading '%v' directory: %v", MAPPING_FILES_PATH, err)
	}

	sortedTreeFiles, err := GetSortedTreeFilesWithDate()
	if err != nil {
		return err
	}

	fileToErrors := make(map[string][]string)
	missingMappings := make(map[string]struct{}, len(sortedTreeFiles)-1)
	for i := 0; i < len(sortedTreeFiles)-1; i++ {
		if sortedTreeFiles[i+1].HasTSV {
			missingMappings[sortedTreeFiles[i].GetDatedFilenameWithoutExtension()] = struct{}{}
		}
	}

	for _, file := range mappingFiles {
		mappingFileName := file.Name()
		if mappingFileName == ".gitkeep" {
			continue
		}

		errors := make([]string, 0)

		// File name validations
		mappedTrees := strings.Split(file.Name(), "_to_")
		mappedFrom := mappedTrees[0]
		mappedTo := strings.Replace(mappedTrees[1], ".txt", "", 1)

		if len(mappedTrees) != 2 {
			errors = append(errors, fmt.Sprintf("Error: mapping file has invalid name: '%v'", mappingFileName))
			continue
		}

		exists := false
		expectedMappedTo := ""
		for i, treeFile := range sortedTreeFiles {
			if treeFile.GetDatedFilenameWithoutExtension() == mappedFrom {
				exists = true
				if i < len(sortedTreeFiles)-1 {
					expectedMappedTo = sortedTreeFiles[i+1].GetDatedFilenameWithoutExtension()
				}
				break
			}
		}

		if !exists {
			errors = append(errors, fmt.Sprintf("Error: mapping for unknown tree '%v' exists", mappedFrom))
		} else if expectedMappedTo == "" {
			errors = append(errors, fmt.Sprintf("Error: '%v' is mapped to '%v', no mapping expected", mappedFrom, mappedTo))
		} else if mappedTo != expectedMappedTo {
			errors = append(errors, fmt.Sprintf("Error: '%v' is mapped to '%v', expected '%v'", mappedFrom, mappedTo, expectedMappedTo))
		}

		// Only validate content once file names are valid
		if len(errors) > 0 {
			fileToErrors[mappingFileName] = errors
			continue
		}
		delete(missingMappings, mappedFrom)

		mappedFromFilename := ""
		mappedToFilename := ""
		for _, file := range sortedTreeFiles {
			if file.GetDatedFilenameWithoutExtension() == mappedFrom {
				mappedFromFilename = file.Name
			} else if file.GetDatedFilenameWithoutExtension() == mappedTo {
				mappedToFilename = file.Name
			}
			if mappedFromFilename != "" && mappedToFilename != "" {
				break
			}
		}

		mappedFromCodes, err := GetCodes(mappedFromFilename)
		if err != nil {
			return err
		}

		mappedToCodes, err := GetCodes(mappedToFilename)
		if err != nil {
			return err
		}

		// Content validations
		mappingFileContent, err := os.ReadFile(filepath.Join(MAPPING_FILES_PATH, mappingFileName))
		if err != nil {
			return fmt.Errorf("Error reading mapping file '%v': %v", mappingFileName, err)
		}

		mappingFileLines := strings.Split(string(mappingFileContent), "\n")
		if len(mappingFileLines) == 0 {
			errors = append(errors, fmt.Sprintf("Error: mapping file '%v' is empty", mappingFileName))
		} else {
			header1, header2, err := parseRow(mappingFileLines[0])
			if err != nil {
				errors = append(errors, "Error: invalid header row")
			} else {
				if header1 != mappedFrom {
					errors = append(errors, fmt.Sprintf("Error: header in column 1 is expected to be '%v' based on mapping file name, got '%v'", mappedFrom, header1))
				}
				if header2 != mappedTo {
					errors = append(errors, fmt.Sprintf("Error: header in column 2 is expected to be '%v' based on mapping file name, got '%v'", mappedTo, header2))
				}
			}

			missingCodes := make(map[string]struct{}, len(mappedFromCodes))
			maps.Copy(missingCodes, mappedFromCodes)

			for i := 1; i < len(mappingFileLines); i++ {
				col1, col2, err := parseRow(mappingFileLines[i])
				if err != nil {
					errors = append(errors, fmt.Sprintf("Error: invalid row on line %v", i+1))
					continue
				}

				_, exists := mappedFromCodes[col1]
				if !exists {
					errors = append(errors, fmt.Sprintf("Error: code in column 1 on line %v not found in '%v', got '%v'", i+1, mappedFrom, col1))
				} else {
					delete(missingCodes, col1)
				}

				_, exists = mappedToCodes[col2]
				if !exists {
					errors = append(errors, fmt.Sprintf("Error: code in column 2 on line %v not found in '%v', got '%v'", i+1, mappedTo, col2))
				}
			}

			if len(missingCodes) > 0 {
				var sb strings.Builder
				for code := range missingCodes {
					if sb.Len() > 0 {
						sb.WriteString(", ")
					}
					sb.WriteString(code)
				}
				errors = append(errors, fmt.Sprintf("Error: codes not mapped from '%v': %v", mappedFrom, sb.String()))
			}
		}

		if len(errors) > 0 {
			fileToErrors[mappingFileName] = errors
		}
	}

	var errorMessage strings.Builder
	if len(missingMappings) > 0 {
		errorMessage.WriteString("General Errors:")
		for key := range missingMappings {
			errorMessage.WriteString(fmt.Sprintf("\n\t* Missing mapping for '%v'", key))
		}
		errorMessage.WriteString("\n")
	}
	for file, errors := range fileToErrors {
		errorMessage.WriteString(fmt.Sprintf("\nErrors for %v:", file))
		for _, err := range errors {
			errorMessage.WriteString(fmt.Sprintf("\n\t* %v", err))
		}
		errorMessage.WriteString("\n")
	}
	if errorMessage.Len() > 0 {
		return errors.New(errorMessage.String())
	}
	return nil
}

func parseRow(row string) (col1 string, col2 string, err error) {
	content := strings.Split(row, "\t")
	if len(content) != 2 {
		return "", "", errors.New("invalid row")
	}
	return content[0], content[1], nil
}

func ValidateTreeDir() error {
	treeFiles, err := os.ReadDir(TREE_FILES_PATH)
	if err != nil {
		return fmt.Errorf("Error reading '%v' directory: %v", TREE_FILES_PATH, err)
	}

	fileToErrors := make(map[string][]string)
	for _, file := range treeFiles {
		if !file.IsDir() {
			errors := make([]string, 0)

			filename := file.Name()
			if _, err := GetDateFromFilename(filename); err != nil &&
				filename != DEV_TREE_IDENTIFIER+".json" &&
				filename != CANDIDATE_TREE_IDENTIFIER+".json" &&
				filename != LEGACY_TREE_IDENTIFIER+".json" {
				errors = append(errors, fmt.Sprintf("Invalid filename: %v", err))
			}

			tree, err := ReadTreeFromFile(filename)
			if err != nil {
				errors = append(errors, err.Error())
			}

			codes := make(map[string]struct{})
			codesWithMultipleNodes := make([]string, 0)
			err = tree.BFS(func(node *TreeNode, _ uint) {
				_, exists := codes[node.Code]
				if exists && !slices.Contains(codesWithMultipleNodes, node.Code) {
					codesWithMultipleNodes = append(codesWithMultipleNodes, node.Code)
				} else {
					codes[node.Code] = struct{}{}
				}
			})
			if err != nil {
				errors = append(errors, err.Error())
			}
			if len(codesWithMultipleNodes) > 0 {
				errors = append(errors, fmt.Sprintf("Error: the following codes have multiple nodes: %v", strings.Join(codesWithMultipleNodes, ", ")))
			}

			if len(errors) > 0 {
				fileToErrors[filename] = errors
			}
		}
	}

	if len(fileToErrors) > 0 {
		var errorMessage strings.Builder
		for file, errors := range fileToErrors {
			errorMessage.WriteString(fmt.Sprintf("\nErrors for %v:", file))
			for _, err := range errors {
				errorMessage.WriteString(fmt.Sprintf("\n\t* %v", err))
			}
			errorMessage.WriteString("\n")
		}
		return errors.New(errorMessage.String())
	}
	return nil
}
