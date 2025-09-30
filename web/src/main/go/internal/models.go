package internal

import (
	"fmt"
)

type Tree map[string]TreeNode

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
	UMLS []string
	NCI  []string
}

func (tree Tree) BFS(onNodeVisited func(node TreeNode)) error {
	nodes := make([]TreeNode, 0)
	for _, node := range tree {
		nodes = append(nodes, node)
	}

	if len(nodes) != 1 {
		return fmt.Errorf("Error: tree has %v root nodes, expected 1", len(nodes))
	}

	for len(nodes) > 0 {
		newNodes := make([]TreeNode, 0)
		for _, node := range nodes {
			onNodeVisited(node)
			for _, newNode := range node.Children {
				newNodes = append(newNodes, newNode)
			}
		}
		nodes = newNodes
	}

	return nil
}
