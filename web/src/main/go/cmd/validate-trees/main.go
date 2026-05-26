package main

import (
	"log"

	"github.com/cBioPortal/oncotree/internal"
)

func main() {
	err := internal.ValidateTreeDir()
	if err != nil {
		log.Fatalf("Error validating tree files: %v", err)
	}
}
