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

var filenameWithDateRegex = regexp.MustCompile(`^oncotree_(\d{4})_(\d{2})_(\d{2})\.(json|txt)$`)

type DatedFile struct {
	Name string
	Date time.Time
}

type DatedFileFormat int

const (
	JSON DatedFileFormat = iota
	TXT
)

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
				filesWithDate = append(filesWithDate, DatedFile{Name: file.Name(), Date: date})
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
				filesWithDate = append(filesWithDate, DatedFile{Name: file.Name(), Date: date})
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

func GetTreeFilepath(name string) string {
	return filepath.Join(TREE_FILES_PATH, name)
}

func (file *DatedFile) GetDatedFilenameWithoutExtension() string {
	name := strings.Replace(file.Name, ".txt", "", 1)
	return strings.Replace(name, ".json", "", 1)
}
