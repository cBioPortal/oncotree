package main

import (
	"log"

	"github.com/cBioPortal/oncotree/internal"
)

func main() {
	err := internal.ValidateMappingDir()
	if err != nil {
		log.Fatalf("Error validating mappings files: %v", err)
	}
}
