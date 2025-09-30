package main

import (
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/cBioPortal/oncotree/internal"
)

func main() {
	filesWithDate, err := internal.GetSortedTreeFilesWithDate()

	for i := range len(filesWithDate) - 1 {
		prevFilename := filesWithDate[i].Name
		nextFilename := filesWithDate[i+1].Name
		mappingFilename := internal.RemoveJsonFilenameExtension(prevFilename) + "_to_" + internal.RemoveJsonFilenameExtension(nextFilename) + ".tsv"
		mappingFilepath := filepath.Join(internal.MAPPING_FILES_PATH, mappingFilename)

		_, err = os.Stat(mappingFilepath)
		if err == nil {
			log.Printf("mapping file '%v' already exists, skipping...", mappingFilename)
			continue
		} else if !errors.Is(err, os.ErrNotExist) {
			log.Fatalf("error getting file info for '%v'", mappingFilename)
		}

		prevTree, err := internal.ReadTreeFromFile(prevFilename)
		if err != nil {
			log.Fatal(err)
		}

		nextTree, err := internal.ReadTreeFromFile(nextFilename)
		if err != nil {
			log.Fatal(err)
		}

		prevTreeCodes, err := internal.GetCodes(prevTree)
		if err != nil {
			log.Fatalf("error retrieving codes from '%v': %v", prevFilename, err)
		}

		nextTreeCodes, err := internal.GetCodes(nextTree)
		if err != nil {
			log.Fatalf("error retrieving codes from '%v': %v", nextFilename, err)
		}

		var mappingFile strings.Builder
		mappingFile.WriteString(fmt.Sprintf("%v\t%v", internal.RemoveJsonFilenameExtension(prevFilename), internal.RemoveJsonFilenameExtension(nextFilename)))
		for code := range prevTreeCodes {
			_, exists := nextTreeCodes[code]
			newCode := ""
			if exists {
				newCode = code
			}
			line := fmt.Sprintf("\n%v\t%v", code, newCode)
			_, err = mappingFile.WriteString(line)
			if err != nil {
				log.Fatalf("error writing line '%v' to mapping file '%v': %v", line, mappingFilename, err)
			}
		}

		err = os.MkdirAll(internal.MAPPING_FILES_PATH, os.ModePerm)
		if err != nil {
			log.Fatalf("error creating mapping files directory: %v", err)
		}

		err = os.WriteFile(mappingFilepath, []byte(mappingFile.String()), os.ModePerm)
		if err != nil {
			log.Fatalf("error writing mapping file '%v': %v", mappingFilename, err)
		}
	}
}
