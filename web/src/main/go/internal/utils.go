package internal

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
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

var filenameWithDateRegex = regexp.MustCompile(`^oncotree_(\d{4})_(\d{2})_(\d{2}).json$`)

type DatedFile struct {
	Name string
	Date time.Time
}

func GetSortedTreeFilesWithDate() ([]DatedFile, error) {
	treeFiles, err := os.ReadDir(TREE_FILES_PATH)
	if err != nil {
		return nil, fmt.Errorf("Error reading '%v' directory: %v", TREE_FILES_PATH, err)
	}

	filesWithDate := make([]DatedFile, 0)
	for _, file := range treeFiles {
		if !file.IsDir() {
			date, err := GetDateFromFilename(file.Name())
			if err == nil {
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

func GetCodes(tree Tree) (map[string]struct{}, error) {
	codes := make(map[string]struct{})
	err := tree.BFS(func(node TreeNode) {
		codes[node.Code] = struct{}{}
	})
	if err != nil {
		return nil, err
	}
	return codes, nil
}

func GetTreeFilepath(name string) string {
	return filepath.Join(TREE_FILES_PATH, name)
}

func RemoveJsonFilenameExtension(name string) string {
	return strings.Replace(name, ".json", "", 1)
}
