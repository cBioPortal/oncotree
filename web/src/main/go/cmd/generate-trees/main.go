package main

import (
	"encoding/csv"
	"errors"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/cBioPortal/oncotree/internal"
)

func main() {
	tsvs, err := os.ReadDir(internal.TSV_FILES_PATH)
	if err != nil {
		log.Fatalf("Error reading '%v' directory: %v", internal.TSV_FILES_PATH, err)
	}

	for _, file := range tsvs {
		if !file.IsDir() {
			CreateOncoTreeFromFile(filepath.Join(internal.TSV_FILES_PATH, file.Name()))
		}
	}
}

func CreateOncoTreeFromFile(path string) (internal.Tree, error) {
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
