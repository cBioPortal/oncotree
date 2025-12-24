package main

import (
	"fmt"
	"log"
	"os"
	"slices"
	"strings"

	"github.com/cBioPortal/oncotree/internal"
)

func main() {
	treeFiles, err := os.ReadDir(internal.TREE_FILES_PATH)
	if err != nil {
		log.Fatalf("Error reading '%v' directory: %v", internal.TREE_FILES_PATH, err)
	}

	fileToErrors := make(map[string][]string)
	for _, file := range treeFiles {
		if !file.IsDir() {
			errors := make([]string, 0)

			filename := file.Name()
			if _, err := internal.GetDateFromFilename(filename); err != nil &&
				filename != internal.DEV_TREE_IDENTIFIER+".json" &&
				filename != internal.CANDIDATE_TREE_IDENTIFIER+".json" &&
				filename != internal.LEGACY_TREE_IDENTIFIER+".json" {
				errors = append(errors, fmt.Sprintf("Invalid filename: %v", err))
			}

			tree, err := internal.ReadTreeFromFile(filename)
			if err != nil {
				errors = append(errors, err.Error())
			}

			codes := make(map[string]struct{})
			codesWithMultipleNodes := make([]string, 0)
			err = tree.BFS(func(node *internal.TreeNode, _ uint) {
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
		log.Fatal(errorMessage.String())
	}
}
