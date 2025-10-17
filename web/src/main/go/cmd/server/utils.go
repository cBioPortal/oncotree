package main

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"slices"
	"sort"
	"strconv"
	"strings"

	"github.com/cBioPortal/oncotree/internal"
)

func GetDefaultTreeVersion() string {
	return "oncotree_latest_stable"
}

// Version represents a single OncoTree version
type Version struct {
	ApiIdentifier string `json:"api_identifier"`
	Description   string `json:"description"`
	ReleaseDate   string `json:"release_date"`
	Visible       bool   `json:"visible"`
}

func getHardcodedVersions() []Version {
	return []Version{
		{
			ApiIdentifier: "oncotree_legacy_1.1",
			Description:   "This is the closest match in TopBraid for the TumorTypes_txt file associated with release 1.1 of OncoTree (approved by committee)",
			ReleaseDate:   "2016-03-28",
			Visible:       false,
		},
		{
			ApiIdentifier: "oncotree_candidate_release",
			Description:   "This version of the OncoTree reflects upcoming changes which have been approved for the next public release of oncotree. It also includes a small number of nodes which will not be included in the next public release (see the news page for more details). The next public release may possibly include additional oncotree nodes, if approved.",
			ReleaseDate:   "2021-11-03",
			Visible:       true,
		},
		{
			ApiIdentifier: "oncotree_development",
			Description:   "Latest OncoTree under development (subject to <b class=text-danger>change without notice</b>)",
			ReleaseDate:   "2021-11-04",
			Visible:       true,
		},
		{
			ApiIdentifier: "oncotree_latest_stable",
			Description:   "This is the latest approved version for public use.",
			ReleaseDate:   "2025-10-03",
			Visible:       true,
		},
	}
}

func getTreeVersions(treeDir string) ([]Version, error) {
	files, err := os.ReadDir(treeDir)
	if err != nil {
		return nil, err
	}

	pattern := regexp.MustCompile(`^oncotree_(\d{4}_\d{2}_\d{2})\.json$`)
	var versions []Version

	versions = append(versions, getHardcodedVersions()...)

	for _, file := range files {
		if file.IsDir() {
			continue
		}
		match := pattern.FindStringSubmatch(file.Name())
		if len(match) == 2 {
			date := match[1]
			releaseDate := strings.ReplaceAll(date, "_", "-")
			apiIdentifier := strings.TrimSuffix(file.Name(), filepath.Ext(file.Name()))

			versions = append(versions, Version{
				ApiIdentifier: apiIdentifier,
				Description:   fmt.Sprintf("Stable OncoTree released on date %s", releaseDate),
				ReleaseDate:   releaseDate,
				Visible:       false,
			})
		}
	}

	sort.Slice(versions, func(i, j int) bool {
		return versions[i].ReleaseDate > versions[j].ReleaseDate
	})

	return versions, nil
}

func GetAvailableVersionIdentifiers(treeDir string) ([]string, error) {
	versions, err := getTreeVersions(treeDir)
	if err != nil {
		return nil, err
	}

	ids := make([]string, 0, len(versions))
	for _, v := range versions {
		ids = append(ids, v.ApiIdentifier)
	}
	return ids, nil
}

func isValidVersion(version string) bool {
	treeDir := "../../../../trees"
	versions, err := GetAvailableVersionIdentifiers(treeDir)
	if err != nil {
		return false
	}
	return slices.Contains(versions, version)
}

func generateTumorTypesTSV(treeFile string) (string, error) {
	tree, err := internal.ReadTreeFromFile(treeFile)
	if err != nil {
		return "", fmt.Errorf("failed to read tree: %w", err)
	}

	var sb strings.Builder

	// Assume only one root node (level 0)
	var root internal.TreeNode
	for _, node := range tree {
		if node.Level == 0 {
			root = node
			break
		}
	}

	// Compute maximum depth of the tree
	maxLevel := getMaxLevel(root)

	// Write TSV header
	for i := 1; i <= maxLevel; i++ {
		sb.WriteString(fmt.Sprintf("level_%d\t", i))
	}
	sb.WriteString("metamaintype\tmetacolor\tmetanci\tmetaumls\thistory\n")

	// Write rows via DFS
	for _, child := range sortChildrenByName(root.Children) {
		writeTSVRows(child, []string{}, &sb)
	}

	return sb.String(), nil
}

func getMaxLevel(node internal.TreeNode) int {
	max := int(node.Level)
	for _, child := range node.Children {
		if level := getMaxLevel(child); level > max {
			max = level
		}
	}
	return max
}

func writeTSVRows(node internal.TreeNode, levels []string, sb *strings.Builder) {
	level := int(node.Level)

	// Resize levels slice
	if len(levels) < level {
		for len(levels) < level {
			levels = append(levels, "")
		}
	} else {
		levels = levels[:level]
	}

	// Set current level
	levels[level-1] = fmt.Sprintf("%s (%s)", node.Name, node.Code)

	// Metadata fields
	metamaintype := safeStr(node.MainType)
	metacolor := safeStr(node.Color)
	metanci := strings.Join(node.ExternalReferences.NCI, ",")
	metaumls := strings.Join(node.ExternalReferences.UMLS, ",")
	history := strings.Join(node.History, ",")

	// Write row
	sb.WriteString(strings.Join(levels, "\t") + "\t")
	sb.WriteString(fmt.Sprintf("%s\t%s\t%s\t%s\t%s\n", metamaintype, metacolor, metanci, metaumls, history))

	// Recurse into sorted children
	for _, child := range sortChildrenByName(node.Children) {
		writeTSVRows(child, levels, sb)
	}
}

func sortChildrenByName(children internal.Tree) []internal.TreeNode {
	nodes := make([]internal.TreeNode, 0, len(children))
	for _, node := range children {
		nodes = append(nodes, node)
	}
	sort.Slice(nodes, func(i, j int) bool {
		return nodes[i].Name < nodes[j].Name
	})
	return nodes
}

func safeStr(s *string) string {
	if s == nil {
		return ""
	}
	return *s
}

// getMainTypes reads a tree file and returns a sorted list of unique main types
func getMainTypes(treeFile string) ([]string, error) {
	tree, err := internal.ReadTreeFromFile(treeFile)
	if err != nil {
		return nil, fmt.Errorf("failed to read tree file: %w", err)
	}

	mainTypeSet := make(map[string]struct{})

	err = tree.BFS(func(node internal.TreeNode) {
		if node.MainType != nil && *node.MainType != "" {
			mainTypeSet[*node.MainType] = struct{}{}
		}
	})
	if err != nil {
		return nil, fmt.Errorf("tree BFS traversal failed: %w", err)
	}

	mainTypes := make([]string, 0, len(mainTypeSet))
	for mt := range mainTypeSet {
		mainTypes = append(mainTypes, mt)
	}
	sort.Strings(mainTypes)

	return mainTypes, nil
}

func flattenTumorTypes(tree internal.Tree) []internal.TreeNode {
	var flat []internal.TreeNode
	_ = tree.BFS(func(node internal.TreeNode) {
		if node.Level > 0 { // exclude root
			node.Children = nil
			flat = append(flat, node)
		}
	})
	return flat
}

func searchTumorTypes(nodes []internal.TreeNode, searchType, query string, exact bool, levelsStr string) []internal.TreeNode {
	levelsMap := parseLevels(levelsStr)
	queryLower := strings.ToLower(query)

	filtered := []internal.TreeNode{}

	for _, node := range nodes {
		value := getValueByType(node, searchType)
		if value == "" {
			continue
		}

		valueLower := strings.ToLower(value)
		if matchQuery(valueLower, queryLower, exact) && levelsMap[node.Level] {
			filtered = append(filtered, node)
		}
	}

	return filtered
}

func parseLevels(levelsStr string) map[uint]bool {
	levelsMap := make(map[uint]bool)
	for _, lvlStr := range strings.Split(levelsStr, ",") {
		lvlStr = strings.TrimSpace(lvlStr)
		lvl, err := strconv.ParseUint(lvlStr, 10, 32)
		if err == nil && lvl != 0 {
			levelsMap[uint(lvl)] = true
		}
	}
	return levelsMap
}

func getValueByType(node internal.TreeNode, searchType string) string {
	switch strings.ToLower(searchType) {
	case "code":
		return node.Code
	case "name":
		return node.Name
	case "maintype":
		if node.MainType != nil {
			return *node.MainType
		}
	case "color":
		if node.Color != nil {
			return *node.Color
		}
	case "level":
		return fmt.Sprint(node.Level)
	case "nci":
		if len(node.ExternalReferences.NCI) > 0 {
			return node.ExternalReferences.NCI[0] // or join all if needed
		}
	case "umls":
		if len(node.ExternalReferences.UMLS) > 0 {
			return node.ExternalReferences.UMLS[0]
		}
	}
	return ""
}

func matchQuery(value, query string, exact bool) bool {
	if exact {
		return value == query
	}
	return strings.Contains(value, query)
}
