package main

import (
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/cBioPortal/oncotree/web/src/main/go/internal"
)

func main() {
	treeFiles, err := os.ReadDir(internal.TREE_FILES_PATH)
	if err != nil {
		log.Fatalf("Error reading '%v' directory: %v", internal.TREE_FILES_PATH, err)
	}

	filesWithDate := make([]DatedFile, 0)
	for _, file := range treeFiles {
		if !file.IsDir() {
			date, err := internal.GetDateFromFilename(file.Name())
			if err == nil {
				filesWithDate = append(filesWithDate, DatedFile{name: file.Name(), date: date})
			}
		}
	}
	sort.Slice(filesWithDate, func(i, j int) bool {
		return filesWithDate[i].date.Before(filesWithDate[j].date)
	})

	for i := range len(filesWithDate) - 1 {
		prevFilename := filesWithDate[i].name
		nextFilename := filesWithDate[i+1].name
		mappingFilename := removeJsonFilenameExtension(prevFilename) + "_to_" + removeJsonFilenameExtension(nextFilename) + ".tsv"
		mappingFilepath := filepath.Join(internal.TREE_FILES_PATH, "mappings", mappingFilename)

		_, err = os.Stat(mappingFilepath)
		if err == nil {
			log.Printf("mapping file '%v' already exists, skipping...", mappingFilename)
			continue
		} else if !errors.Is(err, os.ErrNotExist) {
			log.Fatalf("error getting file info for '%v'", mappingFilename)
		}

		prevTree, err := internal.ReadTreeFromFile(prevFilename)
		if err != nil {
			log.Fatal(err)
		}

		nextTree, err := internal.ReadTreeFromFile(nextFilename)
		if err != nil {
			log.Fatal(err)
		}

		prevTreeCodes, err := getCodes(prevTree)
		if err != nil {
			log.Fatalf("error retrieving codes from '%v': %v", prevFilename, err)
		}

		nextTreeCodes, err := getCodes(nextTree)
		if err != nil {
			log.Fatalf("error retrieving codes from '%v': %v", nextFilename, err)
		}

		var mappingFile strings.Builder
		mappingFile.WriteString(fmt.Sprintf("%v\t%v", removeJsonFilenameExtension(prevFilename), removeJsonFilenameExtension(nextFilename)))
		for code := range prevTreeCodes {
			_, exists := nextTreeCodes[code]
			newCode := ""
			if exists {
				newCode = code
			}
			line := fmt.Sprintf("\n%v\t%v", code, newCode)
			_, err = mappingFile.WriteString(line)
			if err != nil {
				log.Fatalf("error writing line '%v' to mapping file '%v': %v", line, mappingFilename, err)
			}
		}

		err = os.WriteFile(mappingFilepath, []byte(mappingFile.String()), os.ModePerm)
		if err != nil {
			log.Fatalf("error writing mapping file '%v': %v", mappingFilename, err)
		}
	}
}

func getCodes(tree internal.Tree) (map[string]struct{}, error) {
	codes := make(map[string]struct{})
	err := tree.BFS(func(node internal.TreeNode) {
		codes[node.Code] = struct{}{}
	})
	if err != nil {
		return nil, err
	}
	return codes, nil
}

func removeJsonFilenameExtension(name string) string {
	return strings.Replace(name, ".json", "", 1)
}

type DatedFile struct {
	name string
	date time.Time
}
