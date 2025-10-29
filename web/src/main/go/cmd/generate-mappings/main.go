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
		prevFile := filesWithDate[i]
		nextFile := filesWithDate[i+1]
		mappingFilename := prevFile.GetDatedFilenameWithoutExtension() + "_to_" + nextFile.GetDatedFilenameWithoutExtension() + ".tsv"
		mappingFilepath := filepath.Join(internal.MAPPING_FILES_PATH, mappingFilename)

		_, err = os.Stat(mappingFilepath)
		if err == nil {
			log.Printf("mapping file '%v' already exists, skipping...", mappingFilename)
			continue
		} else if !errors.Is(err, os.ErrNotExist) {
			log.Fatalf("error getting file info for '%v'", mappingFilename)
		}

		prevTreeCodes, err := internal.GetCodes(prevFile.Name)
		if err != nil {
			log.Fatalf("error retrieving codes from '%v': %v", prevFile.Name, err)
		}

		nextTreeCodes, err := internal.GetCodes(nextFile.Name)
		if err != nil {
			log.Fatalf("error retrieving codes from '%v': %v", nextFile, err)
		}

		var mappingFile strings.Builder
		mappingFile.WriteString(fmt.Sprintf("%v\t%v", prevFile.GetDatedFilenameWithoutExtension(), nextFile.GetDatedFilenameWithoutExtension()))
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

		err = os.WriteFile(mappingFilepath, []byte(mappingFile.String()), os.ModePerm)
		if err != nil {
			log.Fatalf("error writing mapping file '%v': %v", mappingFilename, err)
		}
	}
}
