package internal

import (
	"os"
	"path/filepath"
)

var TREE_FILES_PATH string
var MAPPING_FILES_PATH string
var TSV_FILES_PATH string

func init() {
	treeDir := os.Getenv("TREE_DIR")
	if treeDir == "" {
		treeDir = "../../../../trees"
	}
	TREE_FILES_PATH, _ = filepath.Abs(treeDir)
	MAPPING_FILES_PATH = filepath.Join(TREE_FILES_PATH, "mappings")
	TSV_FILES_PATH = filepath.Join(TREE_FILES_PATH, "tsv")
}

const (
	LEGACY_TREE_IDENTIFIER        = "oncotree_legacy_1.1"
	CANDIDATE_TREE_IDENTIFIER     = "oncotree_candidate_release"
	DEV_TREE_IDENTIFIER           = "oncotree_development"
	LATEST_STABLE_TREE_IDENTIFIER = "oncotree_latest_stable"
)

const (
	CODE_HEADER      = "Code"
	COLOR_HEADER     = "Color"
	NAME_HEADER      = "Name"
	MAIN_TYPE_HEADER = "MainType"
	UMLS_HEADER      = "UMLS"
	NCI_HEADER       = "NCI"
	TISSUE_HEADER    = "Tissue"
	PARENT_HEADER    = "Parent"
)
