package main

import (
	"encoding/csv"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"slices"
	"strings"

	"github.com/cBioPortal/oncotree/internal"
)

func main() {
	tsvs, err := os.ReadDir(internal.TSV_FILES_PATH)
	if err != nil {
		log.Fatalf("Error reading '%v' directory: %v", internal.TSV_FILES_PATH, err)
	}

	previousCodeGetter := realPreviousCodeGetter{}
	for _, file := range tsvs {
		if !file.IsDir() {
			treeFilename := strings.Replace(file.Name(), ".txt", ".json", 1)
			treeFilepath := internal.GetTreeFilepath(treeFilename)
			_, err = os.Stat(treeFilepath)
			if err == nil {
				log.Printf("tree file '%v' already exists, skipping...", treeFilename)
				continue
			} else if !errors.Is(err, os.ErrNotExist) {
				log.Fatalf("error getting file info for '%v'", treeFilename)
			}

			tree, err := CreateOncoTreeFromFile(filepath.Join(internal.TSV_FILES_PATH, file.Name()), &previousCodeGetter)
			if err != nil {
				log.Fatalf("error creating tree from file '%v': %v", file.Name(), tree)
			}

			treeBytes, err := json.Marshal(tree)
			if err != nil {
				log.Fatalf("error marshalling tree '%v': %v", treeFilename, err)
			}

			err = os.WriteFile(treeFilepath, treeBytes, os.ModePerm)
		}
	}
}

func CreateOncoTreeFromFile(path string, previousCodeGetter PreviousCodeGetter) (internal.Tree, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	reader := csv.NewReader(file)
	reader.Comma = '\t'

	headerRow, err := reader.Read()
	if err == io.EOF {
		return nil, errors.New("missing header row")
	} else if err != nil {
		return nil, fmt.Errorf("error parsing header row: %s", err)
	}

	codeIndex := -1
	colorIndex := -1
	nameIndex := -1
	mainTypeIndex := -1
	umlsIndex := -1
	nciIndex := -1
	tissueIndex := -1
	parentIndex := -1
	for i, colName := range headerRow {
		switch colName {
		case internal.CODE_HEADER:
			codeIndex = i
		case internal.COLOR_HEADER:
			colorIndex = i
		case internal.NAME_HEADER:
			nameIndex = i
		case internal.MAIN_TYPE_HEADER:
			mainTypeIndex = i
		case internal.UMLS_HEADER:
			umlsIndex = i
		case internal.NCI_HEADER:
			nciIndex = i
		case internal.TISSUE_HEADER:
			tissueIndex = i
		case internal.PARENT_HEADER:
			parentIndex = i
		}
	}

	codeToEquivalentCodes, err := previousCodeGetter.GetPreviousCodes(strings.Replace(filepath.Base(path), ".txt", "", 1))
	if err != nil {
		return nil, err
	}

	root := internal.Tree{}
	rowNumber := 1
	codeToChildren := make(map[string]internal.Tree)
	for {
		row, err := reader.Read()
		if err == io.EOF {
			break
		} else if err != nil {
			return nil, fmt.Errorf("error parsing row %v", rowNumber)
		}

		newNode := &internal.TreeNode{}
		code := row[codeIndex]
		newNode.Code = code
		newNode.Color = stringToPointer(row[colorIndex])
		newNode.Name = row[nameIndex]
		newNode.MainType = stringToPointer(row[mainTypeIndex])
		externalReferences := internal.ExternalReferences{}
		if row[umlsIndex] != "" {
			externalReferences.UMLS = strings.Split(row[umlsIndex], ",")
		}
		if row[nciIndex] != "" {
			externalReferences.NCI = strings.Split(row[nciIndex], ",")
		}
		newNode.ExternalReferences = externalReferences
		newNode.Tissue = stringToPointer(row[tissueIndex])
		parentCode := row[parentIndex]
		newNode.Parent = stringToPointer(parentCode)
		newNode.History = []string{}
		newNode.Revocations = []string{}
		newNode.Precursors = codeToEquivalentCodes[code]
		if newNode.Precursors == nil {
			newNode.Precursors = []string{}
		}

		children, exists := codeToChildren[code]
		if !exists {
			children = internal.Tree{}
			codeToChildren[code] = children
		}
		newNode.Children = children
		if parentCode == "" {
			root[code] = newNode
		} else {
			children, exists = codeToChildren[parentCode]
			if !exists {
				children = internal.Tree{}
				codeToChildren[code] = children
			}
			children[code] = newNode
		}
	}

	root.BFS(func(node *internal.TreeNode, depth uint) {
		node.Level = depth
	})

	return root, nil
}

func stringToPointer(str string) *string {
	if str == "" {
		return nil
	}
	return &str
}

type PreviousCodeGetter interface {
	GetPreviousCodes(treeName string) (map[string][]string, error)
}

type realPreviousCodeGetter struct{}

func (previousCodeGetter *realPreviousCodeGetter) GetPreviousCodes(treeName string) (map[string][]string, error) {
	sortedMappingFiles, err := internal.GetSortedMappingFilesWithDate()
	if err != nil {
		return nil, err
	}

	codeToEquivalentCodes := make(map[string][]string)
	firstTree, err := internal.ReadTreeFromFile(sortedMappingFiles[0].OldTree + ".json")
	if err != nil {
		return nil, err
	}

	firstTree.BFS(func(node *internal.TreeNode, _ uint) {
		prevCodes := slices.Concat(node.Revocations, node.Precursors)
		if len(prevCodes) > 0 {
			codeToEquivalentCodes[node.Code] = prevCodes
		}
	})

	for _, mappingFileData := range sortedMappingFiles {
		if mappingFileData.OldTree == treeName {
			break
		}

		mappingFile, err := os.Open(filepath.Join(internal.MAPPING_FILES_PATH, mappingFileData.GetName()))
		if err != nil {
			return nil, err
		}

		mappingFileReader := csv.NewReader(mappingFile)
		mappingFileReader.Comma = '\t'
		_, err = mappingFileReader.Read()
		if err == io.EOF {
			return nil, errors.New("missing header row")
		} else if err != nil {
			return nil, fmt.Errorf("error parsing header row: %s", err)
		}

		rowNumber := 1
		newCodeToEquivalentCodes := make(map[string][]string)
		for {
			row, err := mappingFileReader.Read()
			if err == io.EOF {
				break
			} else if err != nil {
				return nil, fmt.Errorf("error parsing mapping file row %v", rowNumber)
			}

			if row[0] != row[1] {
				newEquivalentCodes, exists := newCodeToEquivalentCodes[row[1]]
				if exists {
					newCodeToEquivalentCodes[row[1]] = append(newEquivalentCodes, row[0])
				} else {
					newCodeToEquivalentCodes[row[1]] = []string{row[0]}
				}

				codesToAdd, exists := codeToEquivalentCodes[row[0]]
				if exists {
					equivalentCodes, exists := codeToEquivalentCodes[row[1]]
					if exists {
						codeToEquivalentCodes[row[1]] = append(equivalentCodes, codesToAdd...)
					} else {
						codeToEquivalentCodes[row[1]] = codesToAdd
					}
				}
			}
			rowNumber++
		}
		mappingFile.Close()

		for code, newEquivalentCodes := range newCodeToEquivalentCodes {
			equivalentCodes, exists := codeToEquivalentCodes[code]
			if exists {
				codeToEquivalentCodes[code] = append(equivalentCodes, newEquivalentCodes...)
			} else {
				codeToEquivalentCodes[code] = newEquivalentCodes
			}
		}
	}

	return codeToEquivalentCodes, nil
}
