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
	files := os.Args[1:]

	if len(files) != 1 {
		fmt.Fprintf(os.Stderr, "Error: Invalid number of arguments.\n")
		fmt.Fprintf(os.Stderr, "Expected: 1 TSV file path\n")
		fmt.Fprintf(os.Stderr, "Received: %d file(s): %v\n", len(files), strings.Join(files, ", "))
		os.Exit(1)
	}
	file := filepath.Join(internal.TSV_FILES_PATH, filepath.Base(files[0]))
	filename := filepath.Base(files[0])
	jsonFilename := strings.Replace(filename, ".txt", ".json", 1)

	// only dev and candidate can be overwritten
	if filename == internal.DEV_TREE_IDENTIFIER+".txt" || filename == internal.CANDIDATE_TREE_IDENTIFIER+".txt" {
		tree, err := CreateOncoTreeFromFile(file, MockPreviousCodeGetter{})
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error creating tree from %v: %v\n", file, err)
			os.Exit(1)
		}

		treeBytes, err := json.Marshal(tree)
		if err != nil {
			log.Fatalf("Error marshalling tree created from %v: %v\n", file, err)
			os.Exit(1)
		}

		err = os.WriteFile(filepath.Join(internal.TREE_FILES_PATH, jsonFilename), treeBytes, os.ModePerm)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error writing file %v: %v", jsonFilename, err)
			os.Exit(1)
		}
		os.Exit(0)
	}

	_, err := os.Stat(filepath.Join(internal.TREE_FILES_PATH, jsonFilename))
	if err == nil {
		fmt.Fprintf(os.Stderr, "Error: JSON tree already exists for %v\n", file)
		os.Exit(1)
	}

	newTreeDate, err := internal.GetDateFromFilename(filename)
	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}

	sortedTreeFiles, err := internal.GetSortedTreeFilesWithDate()
	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
	mostRecentTree := sortedTreeFiles[len(sortedTreeFiles)-2] // most recent will be the one just uploaded

	if newTreeDate.Before(mostRecentTree.Date) {
		fmt.Fprintf(os.Stderr, "Error: New tree date %v must be newer than the most recent tree date %v\n", newTreeDate, mostRecentTree.Date)
		os.Exit(1)
	}

	mostRecentTreeCodes, err := internal.GetCodes(mostRecentTree.Name)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error retrieving codes from file %v: %v", mostRecentTree.Name, err)
		os.Exit(1)
	}

	newTreeCodes, err := internal.GetCodes(filename)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error retrieving codes from file %v: %v", filename, err)
		os.Exit(1)
	}

	mappingFilename := mostRecentTree.GetDatedFilenameWithoutExtension() + "_to_" + filename
	_, err = os.Stat(filepath.Join(internal.MAPPING_FILES_PATH, mappingFilename))
	if err != nil { // Mapping file does not exist
		var mappingFile strings.Builder
		mappingFile.WriteString(fmt.Sprintf("%v\t%v", mostRecentTree.GetDatedFilenameWithoutExtension(), strings.Replace(filename, ".txt", "", 1)))
		for code := range mostRecentTreeCodes {
			_, exists := newTreeCodes[code]
			newCode := ""
			if exists {
				newCode = code
			}
			line := fmt.Sprintf("\n%v\t%v", code, newCode)
			_, err = mappingFile.WriteString(line)
			if err != nil {
				log.Fatalf("Error writing line %v to mapping file %v: %v", line, mappingFilename, err)
			}
		}

		err = os.WriteFile(filepath.Join(internal.MAPPING_FILES_PATH, mappingFilename), []byte(mappingFile.String()), os.ModePerm)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error writing file %v: %v", mappingFilename, err)
			os.Exit(1)
		}
	}

	// Now we have to validate

	// tree, err := CreateOncoTreeFromFile(file, realPreviousCodeGetter{})
	// if err != nil {
	// 	fmt.Fprintf(os.Stderr, "Error creating tree from %v: %v\n", file, err)
	// 	os.Exit(1)
	// }

	// treeBytes, err := json.Marshal(tree)
	// if err != nil {
	// 	fmt.Fprintf(os.Stderr, "Error marshalling tree created from %v: %v\n", file, err)
	// 	os.Exit(1)
	// }

	// err = os.WriteFile(filepath.Join(OUTPUT_DIR, jsonFilename), treeBytes, os.ModePerm)
	// if err != nil {
	// 	fmt.Fprintf(os.Stderr, "Error writing file %v: %v", jsonFilename, err)
	// 	os.Exit(1)
	// }

	os.Exit(0)
}

type PreviousCodeGetter interface {
	GetPreviousCodes(treeName string) (map[string][]string, error)
}

type MockPreviousCodeGetter struct{}

func (previousCodeGetter MockPreviousCodeGetter) GetPreviousCodes(treeName string) (map[string][]string, error) {
	return make(map[string][]string), nil
}

type realPreviousCodeGetter struct{}

func (previousCodeGetter realPreviousCodeGetter) GetPreviousCodes(treeName string) (map[string][]string, error) {
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
		code := strings.TrimSpace(row[codeIndex])
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
		parentCode := strings.TrimSpace(row[parentIndex])
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
				codeToChildren[parentCode] = children
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
