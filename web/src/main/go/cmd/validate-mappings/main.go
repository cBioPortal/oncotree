package main

import (
	"errors"
	"fmt"
	"log"
	"maps"
	"os"
	"path/filepath"
	"strings"

	"github.com/cBioPortal/oncotree/internal"
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
	missingMappings := make(map[string]struct{}, len(sortedTreeFiles)-1)
	for i := 0; i < len(sortedTreeFiles)-1; i++ {
		missingMappings[sortedTreeFiles[i].GetDatedFilenameWithoutExtension()] = struct{}{}
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

		mappedFromCodes, err := internal.GetCodes(mappedFromFilename)
		if err != nil {
			log.Fatal(err)
		}

		mappedToCodes, err := internal.GetCodes(mappedToFilename)
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
		log.Fatal(errorMessage.String())
	}
}

func parseRow(row string) (col1 string, col2 string, err error) {
	content := strings.Split(row, "\t")
	if len(content) != 2 {
		return "", "", errors.New("invalid row")
	}
	return content[0], content[1], nil
}
