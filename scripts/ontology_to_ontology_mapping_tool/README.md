## Ontology to Ontology Mapping Tool

The Ontology Mapping tool was developed to facilitate the mapping between different cancer classification systems. We currently allow the mappings between OncoTree, ICD-O, NCIt, UMLS and HemeOnc systems.

### Prerequisites
The Ontology Mapping tool runs on python 3 and requires `pandas` and `requests` libraries. These libraries can be installed using
```
pip3 install pandas
pip3 install requests
 ```

### Running the tool

The tool can be run with the following command:
```
python <path/to/scripts/ontology_to_ontology_mapping_tool.py> --source-file <path/to/source/file> --target-file <path/to/target/file> --target-code <target_ontology_code>
```

**Options**
```
 -i | --source-file: This is the source file path. The source file must contain one of the ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE in the file header and it must contain codes corresponding to the Ontology System.
 -o | --target-file: This is the path to the target file that will be generated. It will contain ontologies mapped from source code in <source-file> to <target-code>.
 -s | --source-code: This is the source ontology code in <source-file>. It must be one of the ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE.
 -t | --target-code: This is the target ontology code that the script will attempt to map the source file ontology code to. It must be one of the ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE.
```

**Note**
- The source file should be tab delimited and should contain one of the ontology: ONCOTREE_CODE, NCIT_CODE, UMLS_CODE, ICDO_TOPOGRAPHY_CODE, ICDO_MORPHOLOGY_CODE or HEMEONC_CODE in the file header.
- We currently are allowing only one ontology to another ontology mapping. In the future, we plan to extend the tool to support mapping to multiple ontology systems.
