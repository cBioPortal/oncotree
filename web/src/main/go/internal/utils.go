package internal

import (
	"encoding/csv"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"slices"
	"sort"
	"strconv"
	"strings"
	"time"
	"net/http"
)

func ReadTreeFromFile(name string) (Tree, error) {
	treeBytes, err := os.ReadFile(GetTreeFilepath(name))
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

func (file *DatedFile) GetDatedFilenameWithoutExtension() string {
	name := strings.Replace(file.Name, ".txt", "", 1)
	return strings.Replace(name, ".json", "", 1)
}

func GetTreeFilepath(name string) string {
	appEnv := os.Getenv("APP_ENV")

	if name == DEV_TREE_IDENTIFIER+".json" && appEnv == "production" {
		devTreePath := filepath.Join(TREE_FILES_PATH, DEV_TREE_IDENTIFIER+".json")
		_ = fetchDevTreeIfChanged(devTreePath)
	}

	return filepath.Join(TREE_FILES_PATH, name)
}

func fetchDevTreeIfChanged(devTreePath string) error {
	etagPath := devTreePath + ".etag"

	req, err := http.NewRequest("GET", DEV_TREE_GITHUB_RAW_URL, nil)
	if err != nil {
		return err
	}

	// Include If-None-Match header if we have a saved ETag
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
		return nil // already up-to-date

	case http.StatusOK:
		tmp := devTreePath + ".tmp"
		out, err := os.Create(tmp)
		if err != nil {
			return err
		}
		defer out.Close()

		if _, err := io.Copy(out, resp.Body); err != nil {
			return err
		}

		if err := os.Rename(tmp, devTreePath); err != nil {
			return err
		}

		if etag := resp.Header.Get("ETag"); etag != "" {
			_ = os.WriteFile(etagPath, []byte(etag), 0644)
		}
		return nil

	default:
		return fmt.Errorf("unexpected status from GitHub: %s", resp.Status)
	}
}
