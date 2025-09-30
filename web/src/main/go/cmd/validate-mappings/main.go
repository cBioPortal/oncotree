package main

import (
	"errors"
	"fmt"
	"log"
	"maps"
	"os"
	"path/filepath"
	"strings"

	"github.com/cBioPortal/oncotree/web/src/main/go/internal"
)

func main() {
	mappingFiles, err := os.ReadDir(internal.MAPPING_FILES_PATH)
	if err != nil {
		log.Fatalf("Error reading '%v' directory: %v", internal.MAPPING_FILES_PATH, err)
	}

	sortedTreeFiles, err := internal.GetSortedTreeFilesWithDate()
	if err != nil {
		log.Fatal(err)
	}

	fileToErrors := make(map[string][]string)
	for _, file := range mappingFiles {
		mappingFileName := file.Name()
		errors := make([]string, 0)

		// File name validations
		mappedTrees := strings.Split(file.Name(), "_to_")
		mappedFrom := mappedTrees[0]
		mappedTo := strings.Replace(mappedTrees[1], ".tsv", "", 1)

		if len(mappedTrees) != 2 {
			errors = append(errors, fmt.Sprintf("Error: mapping file has invalid name: '%v'", mappingFileName))
			continue
		}

		exists := false
		expectedMappedTo := ""
		for i, treeFile := range sortedTreeFiles {
			if internal.RemoveJsonFilenameExtension(treeFile.Name) == mappedFrom {
				exists = true
				if i < len(sortedTreeFiles)-1 {
					expectedMappedTo = internal.RemoveJsonFilenameExtension(sortedTreeFiles[i+1].Name)
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

		mappedFromTree, err := internal.ReadTreeFromFile(mappedFrom + ".json")
		if err != nil {
			log.Fatal(err)
		}

		mappedToTree, err := internal.ReadTreeFromFile(mappedTo + ".json")
		if err != nil {
			log.Fatal(err)
		}

		mappedFromCodes, err := internal.GetCodes(mappedFromTree)
		if err != nil {
			log.Fatal(err)
		}

		mappedToCodes, err := internal.GetCodes(mappedToTree)
		if err != nil {
			log.Fatal(err)
		}

		// Content validations
		mappingFileContent, err := os.ReadFile(filepath.Join(internal.MAPPING_FILES_PATH, mappingFileName))
		if err != nil {
			log.Fatalf("error reading mapping file '%v': %v", mappingFileName, err)
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

	for key, val := range fileToErrors {
		fmt.Println(key)
		for _, err := range val {
			println(err)
		}
		fmt.Println()
	}
}

func parseRow(row string) (col1 string, col2 string, err error) {
	content := strings.Split(row, "\t")
	if len(content) != 2 {
		return "", "", errors.New("invalid row")
	}
	return content[0], content[1], nil
}
