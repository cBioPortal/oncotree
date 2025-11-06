package main

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
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

// LatestStableFile returns the filename and release date of the latest stable OncoTree file.
func LatestStableVersionFile() (string, string, error) {
	datedFiles, err := internal.GetSortedTreeFilesWithDate()
	if err != nil {
		return "", "", fmt.Errorf("failed to read tree files: %w", err)
	}

	latest := datedFiles[len(datedFiles)-1]

	latestVersion := strings.TrimSuffix(latest.Name, filepath.Ext(latest.Name))
	releaseDate := latest.Date.Format("2006-01-02")

	return latestVersion, releaseDate, nil
}

func getHardcodedVersions() []Version {
	_, latestReleaseDate, _ := LatestStableVersionFile()
	return []Version{
		{
			ApiIdentifier: internal.LEGACY_TREE_IDENTIFIER,
			Description:   "This is the closest match in TopBraid for the TumorTypes_txt file associated with release 1.1 of OncoTree (approved by committee)",
			ReleaseDate:   "2016-03-28",
			Visible:       false,
		},
		{
			ApiIdentifier: internal.CANDIDATE_TREE_IDENTIFIER,
			Description:   "This version of the OncoTree reflects upcoming changes which have been approved for the next public release of oncotree. It also includes a small number of nodes which will not be included in the next public release (see the news page for more details). The next public release may possibly include additional oncotree nodes, if approved.",
			ReleaseDate:   "2021-11-03",
			Visible:       true,
		},
		{
			ApiIdentifier: internal.DEV_TREE_IDENTIFIER,
			Description:   "Latest OncoTree under development (subject to <b class=text-danger>change without notice</b>)",
			ReleaseDate:   "2021-11-04",
			Visible:       true,
		},
		{
			ApiIdentifier: internal.LATEST_STABLE_TREE_IDENTIFIER,
			Description:   "This is the latest approved version for public use.",
			ReleaseDate:   latestReleaseDate,
			Visible:       true,
		},
	}
}

func getTreeVersions() ([]Version, error) {
	files, err := os.ReadDir(TreeDir)
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

func resolveAndValidateVersion(version string) (string, error) {
	if version == "oncotree_latest_stable" {
		latestVersion, _, err := LatestStableVersionFile()
		if err != nil {
			return "", err
		}
		if latestVersion == "" {
			return "", fmt.Errorf("no stable version found")
		}
		version = latestVersion
	} else {
		filename := filepath.Join(TreeDir, fmt.Sprintf("%s.json", version))
		if _, err := os.Stat(filename); err != nil {
			if os.IsNotExist(err) {
				return "", fmt.Errorf("invalid version: %s (file not found)", version)
			}
			return "", fmt.Errorf("error accessing version file: %w", err)
		}
	}

	return version, nil
}

func generateTumorTypesTSV(treeFile string) (string, error) {
	tree, err := internal.ReadTreeFromFile(treeFile)
	if err != nil {
		return "", fmt.Errorf("failed to read tree: %w", err)
	}

	// Find root node (Level 0)
	var root internal.TreeNode
	for _, node := range tree {
		if node.Level == 0 {
			root = *node
			break
		}
	}

	var rows []string
	var maxLevel int

	// Recursive DFS traversal
	var dfs func(node internal.TreeNode, levels []string)
	dfs = func(node internal.TreeNode, levels []string) {
		level := int(node.Level)
		if len(levels) < level {
			for len(levels) < level {
				levels = append(levels, "")
			}
		} else {
			levels = levels[:level]
		}

		if node.Level > 0 {
			levels[level-1] = fmt.Sprintf("%s (%s)", node.Name, node.Code)
			rows = append(rows, nodeToTSVRow(node, levels))
			if level > maxLevel {
				maxLevel = level
			}
		}

		// Sort children before recursion
		for _, child := range sortedChildren(node.Children) {
			dfs(child, levels)
		}
	}

	dfs(root, nil)

	// Build TSV output
	var sb strings.Builder
	for i := 1; i <= maxLevel; i++ {
		sb.WriteString(fmt.Sprintf("level_%d\t", i))
	}
	sb.WriteString("metamaintype\tmetacolor\tmetanci\tmetaumls\thistory\n")

	for _, row := range rows {
		sb.WriteString(row)
		sb.WriteByte('\n')
	}

	return sb.String(), nil
}

// Helper to convert node.Children (a map) into a sorted slice by name
func sortedChildren(children internal.Tree) []internal.TreeNode {
	nodes := make([]internal.TreeNode, 0, len(children))
	for _, node := range children {
		nodes = append(nodes, *node)
	}
	sort.Slice(nodes, func(i, j int) bool {
		return nodes[i].Name < nodes[j].Name
	})
	return nodes
}

func nodeToTSVRow(node internal.TreeNode, levels []string) string {
	metamaintype := safeStr(node.MainType)
	metacolor := safeStr(node.Color)
	metanci := strings.Join(node.ExternalReferences.NCI, ",")
	metaumls := strings.Join(node.ExternalReferences.UMLS, ",")
	history := strings.Join(node.History, ",")

	return fmt.Sprintf("%s\t%s\t%s\t%s\t%s\t%s",
		strings.Join(levels, "\t"),
		metamaintype, metacolor, metanci, metaumls, history,
	)
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

	err = tree.BFS(func(node *internal.TreeNode, _ uint) {
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
	_ = tree.BFS(func(node *internal.TreeNode, _ uint) {
		if node.Level > 0 { // exclude root
			children := node.Children
			node.Children = internal.Tree{}
			flat = append(flat, *node)
			node.Children = children
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
	case "mainType":
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
