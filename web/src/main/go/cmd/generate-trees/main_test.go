package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"slices"
	"testing"

	"github.com/cBioPortal/oncotree/internal"
)

func TestCreateOncoTreeFromFile(t *testing.T) {
	const testFile = "test-tsv.txt"

	path, err := os.Getwd()
	if err != nil {
		t.Errorf("Error getting current working directory: %v", err)
	}

	tree, err := CreateOncoTreeFromFile(filepath.Join(path, testFile), &MockPreviousCodeGetter{})
	if err != nil {
		t.Errorf("Error creating OncoTree from file: %v", err)
	}

	expectedLevels := map[uint][]string{
		0: {"TUMOR"},
		1: {"LGG", "MEL", "GBM"},
		2: {"SKCM", "UVM"},
	}
	actualLevels := make(map[uint][]string)
	tree.BFS(func(node *internal.TreeNode, depth uint) {
		levels, exists := actualLevels[depth]
		if exists {
			levels = append(levels, node.Code)
		} else {
			levels = []string{node.Code}
		}
		actualLevels[depth] = levels
	})

	if len(actualLevels) != len(expectedLevels) {
		reportError(t, expectedLevels, actualLevels)
	} else {
		for key := range expectedLevels {
			if !compareSlicesAnyOrder(actualLevels[key], expectedLevels[key]) {
				reportError(t, expectedLevels, actualLevels)
				break
			}
		}
	}

	tr, _ := json.Marshal(tree)
	fmt.Println(string(tr))
}

type MockPreviousCodeGetter struct{}

func (previousCodeGetter *MockPreviousCodeGetter) GetPreviousCodes(treeName string) (map[string][]string, error) {
	return make(map[string][]string), nil
}

func reportError(t *testing.T, expected map[uint][]string, actual map[uint][]string) {
	t.Errorf("Expected BFS result to be equal: expected '%v', got '%v'", expected, actual)
}

func compareSlicesAnyOrder(s1 []string, s2 []string) bool {
	if len(s1) != len(s2) {
		return false
	}

	for _, item := range s1 {
		if !slices.Contains(s2, item) {
			return false
		}
	}
	return true
}
