# OncoTree to OncoTree Mapping Tool

The OncoTree Mapping tool was developed to facilitate the mapping of OncoTree codes between different OncoTree release versions. Instructions for running the tool can be found below in the [Running the tool](#running-the-tool) section. Background information on the Oncotree history modeling and examples of use cases can be found in the [OncoTree History Modeling documentation](/docs/OncoTree-History-Modeling.md). Additional resources such as sample input, output, and mapping summary files can be found in the [Additional Resources](#additional-resources) sections.

[test](#granular-choices)
## [Setting up and downloading the tool](#setting-up-and-downloading-the-tool)

Click here to download the script: <span style="font-size:1.5em; font-weight:bold;">[oncotree_to_oncotree.py &#x2B07;](http://oncotree.mskcc.org/downloads/oncotree_to_oncotree.py)</span>

The OncoTree Mapping tool was written in `python`. To use the tool, please install the following python module if it is not already installed:

- [requests](http://docs.python-requests.org/en/v2.7.0/user/install/)

## [Running the tool](#running-the-tool)

The OncoTree Mapping tool can be run with the following command:


```
python <path/to/scripts/oncotree_to_oncotree.py> --source-file <path/to/source/file> --target-file <path/to/target/file> --source-version <source_oncotree_version> --target-version <target_oncotree_version>
```

**Options**
- `-i | --source-file`: This is the source clinical file path. It must contain `ONCOTREE_CODE` in the file header and it must contain OncoTree codes corresponding to the `<source_oncotree_version>`. Read more about the cBioPortal clinical file format [here](https://docs.cbioportal.org/5.1-data-loading/data-loading/file-formats#clinical-data).
- `-o | --target-file`: This is the path to the target clinical file that will be generated. It will contain mapped OncoTree codes from `<source_oncotree_version>`-to-`<target_oncotree_version>`.
- `-s | --source-version`: This is the source OncoTree version. The OncoTree codes in the source file must correspond to this version.
- `-t | --target-version`: This is the target OncoTree version that the script will attempt to map the source file OncoTree codes to.

The list of OncoTree versions available are viewable [here](http://oncotree.mskcc.org/api/versions) on the dropdown menu [OncoTree home page](http://oncotree.mskcc.org/#/home).

## [Output](#output)

 The OncoTree Mapper Tool will automatically replace the value in the `ONCOTREE_CODE` column with the mapped code if available. The tool will also add a new column called `ONCOTREE_SUGGESTIONS` containing suggestions for OncoTree codes if one or more nodes could not be direclty mapped. The `ONCOTREE_SUGGESTIONS` column formats its suggestions differently depending on the mapping results. Possible suggestion formats and corresponding examples are shown below.
 
 ### 1. Unambiguous Direct Mappings
 Unambiguous direct mappings occur when an oncotree code maps directly to a single code in the target version. In this case, the `ONCOTREE_SUGGESTIONS` column will be left blank, and the mapped code will be automatically placed in the `ONCOTREE_CODE` column. Unambiguous direct mappings are checked for addition of more granular nodes; to see how this may affect the `ONCOTREE_SUGGESTIONS` column formatting, please refer to the section on [More Granular Nodes Introduced](#granular-choices).

### 2. Ambiguous Direct Mappings 
 Ambiguous direct mappings occur when an oncotree code maps to multiple codes in the target version. The `ONCOTREE_SUGGESTIONS` column formats the output as follows:
 
 > _'Source Code' -> {'Code 1', 'Code 2', 'Code 3', ...}_  
 
 **Example: Schema describing the revocation of OncoTree node ALL is mapped to multiple nodes.**

![Example 1](http://oncotree.mskcc.org/images/example_1.png)

> In `oncotree_2018_05_01`, `ALL` had two children: `TALL` and `BALL`. On release `oncotree_2018_06_01`, the ALL node was discontinued and the `TALL` node was renamed `TLL` and the `BALL` node was renamed `BLL`. 

**The `ONCOTREE_SUGGESTIONS` column would be shown as follows:**  
> _ALL -> {TLL, BLL}_
  
### 2. No Direct Mappings   
 No direct mappings occur when the source oncotree code is unrelated to any oncotree code in  the target version. One such possibility is mapping a newly introduced oncotree code backwards in time. In this case, the tool finds the closest set of **neighbors** (e.g parents and children) which are mappable in the target version. The `ONCOTREE_SUGGESTIONS` column returns the set with the keyword **Neighbors** as follows:  
 
 > _'Source Code' -> **Neighbors** {'Code 1', 'Code 2', 'Code 3', ...}_  
 
 **Example: Schema describing a case where new OncoTree node UPA cannot be directly mapped backwards to a node.**

![Example 2](https://raw.githubusercontent.com/averyniceday/oncotree/doc-expansion/docs/images/example_2.png)

> In `oncotree_2019_03_01`, `UPA` was added to the oncotree as a child node of `BLADDER`. Because `UPA` did not exist in previous verison `oncotree_2018_05_01` and did not replace any existing node, the tool uses the surrounding nodes when mapping backwards. In this case, the parent node `BLADDER` is returned as the closest match.

**The `ONCOTREE_SUGGESTIONS` column would be shown as follows:**  
> _UPA -> Neighbors {BLADDER}_

### 3. [More Granular Nodes Introduced](#granular-choices)
In certain cases, the target version can also introduce nodes with more specfic descriptions. When this occurs, the tool will add the string `more granular choices introduced` to the existing text in the `ONCOTREE_SUGGESTIONS` column as follows:  
  
> _'Source Code' -> {'Code 1', ...}, **more granular choices introduced**_  

 **Example: Schema describing a case where OncoTree node TALL is mapped to a node with more granular children**  
 
![Example 3](https://raw.githubusercontent.com/averyniceday/oncotree/doc-expansion/docs/images/example_3.png)

> In `oncotree_2019_03_01`, `TALL` was a leaf node with no children. In release `oncotree_2019_06_01`, `TLL` was introduced as a replacement for `TALL` with additional children `ETPLL` and `NKCLL`. 

**The `ONCOTREE_SUGGESTIONS` column would be shown as follows:** 
> _TALL -> {TLL}, more granular choices introduced_  

### 4. Invalid Source OncoTree Code  
 An invalid souce OncoTree Code means the provided code cannot be found in the source version. In such a case, mapping cannot be attempted and the `ONCOTREE_SUGGESTSIONS` column displays the following:  
 
 > _'Source Code' -> ???, Oncotee code not in source oncotree version_ 
 
## [Additional Resources](#additional-resources)
- [sample input file](https://raw.githubusercontent.com/averyniceday/oncotree/doc-expansion/docs/data_clinical_sample_test.txt)
- [sample output file](https://raw.githubusercontent.com/averyniceday/oncotree/doc-expansion/docs/data_clinical_sample_test_remapped.txt)
- [sample mapping summary report](https://raw.githubusercontent.com/averyniceday/oncotree/doc-expansion/docs/data_clinical_sample_test_remapped_summary.html)
