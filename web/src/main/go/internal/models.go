package internal

import (
	"bytes"
	"encoding/json"
	"fmt"
	"sort"
)

type Tree map[string]*TreeNode

type TreeNode struct {
	Code               string             `json:"code"`
	Color              *string            `json:"color"`
	Name               string             `json:"name"`
	MainType           *string            `json:"mainType"`
	ExternalReferences ExternalReferences `json:"externalReferences"`
	Tissue             *string            `json:"tissue"`
	Children           Tree               `json:"children"`
	Parent             *string            `json:"parent"`
	History            []string           `json:"history"`
	Level              uint               `json:"level"`
	Revocations        []string           `json:"revocations"`
	Precursors         []string           `json:"precursors"`
}

type ExternalReferences struct {
	UMLS []string `json:"UMLS,omitempty"`
	NCI  []string `json:"NCI,omitempty"`
}

func (tree Tree) MarshalJSON() ([]byte, error) {
	codes := make([]string, 0, len(tree))
	for code := range tree {
		codes = append(codes, code)
	}

	sort.Slice(codes, func(i, j int) bool {
		if tree[codes[i]].Name == "Precursor Lesions" {
			return false
		}
		if tree[codes[j]].Name == "Precursor Lesions" {
			return true
		}
		return codes[i] < codes[j]
	})

	var b bytes.Buffer
	b.WriteByte('{')
	for i, code := range codes {
		if i > 0 {
			b.WriteByte(',')
		}

		key, err := json.Marshal(code)
		if err != nil {
			return nil, err
		}
		b.Write(key)
		b.WriteByte(':')

		node, err := json.Marshal(tree[code])
		if err != nil {
			return nil, err
		}
		b.Write(node)
	}
	b.WriteByte('}')

	return b.Bytes(), nil
}

func (tree Tree) BFS(onNodeVisited func(node *TreeNode, depth uint)) error {
	nodes := make([]*TreeNode, 0)
	for _, node := range tree {
		nodes = append(nodes, node)
	}

	if len(nodes) != 1 {
		return fmt.Errorf("Error: tree has %v root nodes, expected 1", len(nodes))
	}

	var depth uint = 0
	for len(nodes) > 0 {
		newNodes := make([]*TreeNode, 0)
		for _, node := range nodes {
			onNodeVisited(node, depth)
			for _, newNode := range node.Children {
				newNodes = append(newNodes, newNode)
			}
		}
		nodes = newNodes
		depth++
	}

	return nil
}
