# OncoTree to OncoTree Mapping Tool

The OncoTree Mapping tool was developed to facilitate the mapping of OncoTree codes between different OncoTree release versions. Instructions for running the tool can be found below in the [Running the tool](#running-the-tool) section. Background information on the Oncotree history modeling and examples of use cases can be found in the [OncoTree History Modeling](#oncotree-history-modeling) section.

## [Setting up and downloading the tool](#setting-up-and-downloading-the-tool)

Follow this link to download the script: [**oncotree_to_oncotree.py**](http://oncotree.mskcc.org/downloads/oncotree_to_oncotree.py)

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

 The OncoTree Mapper Tool will add a new column called `ONCOTREE_SUGGESTIONS` containing suggestions for OncoTree codes if a direct mapping was not available. The `ONCOTREE_SUGGESTIONS` column formats its suggestions differently depending on the mapping results. Possible suggestion formats and corresponding examples are shown below.
 
 **1. Ambiguous Direct Mappings**  
 Ambiguous direct mappings occur when an oncotree code maps to multiple codes in a different version. The `ONCOTREE_SUGGESTIONS` column formats the output with they key word **Choices** as follows:
 
 > _'Source Code' -> **Choices** {'Code 1', 'Code 2', 'Code 3', ...}_  
 
 **Example: Schema describing the revocation of OncoTree node ALL in a later OncoTree release version.**

![Example 1](http://oncotree.mskcc.org/images/example_1.png)

> In `oncotree_2018_05_01`, `ALL` had two children: `TALL` and `BALL`. On release `oncotree_2018_06_01`, the ALL node was discontinued and the `TALL` node was renamed `TLL` and the `BALL` node was renamed `BLL`. 

**The `ONCOTREE_SUGGESTIONS` column would be shown as follows:**  

> _ALL -> Choices {TLL, BLL}_

**2. No Direct Mappings**  
 No direct mappings occur when the source oncotree code is unrelated to any oncotree code in  the target version. One such possibility is mapping a newly introduced oncotree code backwards in time. In this case, the tool finds the closest set of **neighbors** (e.g parents and children) which are mappable in the target version. The `ONCOTREE_SUGGESTIONS` column returns the set with the keyword **Neighbors** as follows:  
 
 > _'Source Code' -> **Neighbors** {'Code 1', 'Code 2', 'Code 3', ...}_  
 
 **Example: Schema describing a case where new OncoTree node <__placeholder__> cannot be directly mapped.**

![Example 1](http://oncotree.mskcc.org/images/example_1.png)

> In `oncotree_2018_06_01`, `ALL` was added to the oncotree. Because `ALL` did not exist in previous verison `oncotree_2018_03_01` and did not replace any existing node, its neighbors are used as closest possible mappings.

**The `ONCOTREE_SUGGESTIONS` column would be shown as follows:**  

> _ALL -> Neighbors {TLL, BLL}_

## [OncoTree History Modeling](#oncotree-history-modeling)

### Background

The OncoTree is an evolving ontology and as such has gone through several iterations since its conception to improve the standardization of cancer type diagnoses from a clinical perspective. As such, some nodes on the OncoTree possess a complex revision history whereas others have a much more simple revision history, such as a simple renaming of the detailed cancer type name.

The complexity of the revision history for some OncoTree nodes created a need for a tool that facilitates the mapping of OncoTree codes between different OncoTree release versions.

### OncoTree history modeling

Three new properties were introduced as a result of the OncoTree history modeling. Each node will now contain the following new properties and will be discussed in further detail below.

**1. History:** A list of _synonymous_  OncoTree codes
**2. Revocations:** A list of OncoTree codes which the current node _was_ a part of the meaning of but have since been revoked from subsequent Oncotree release versions
**3. Precursors:** A list of OncoTree codes which _became_ a part of the meaning of the current node

***

The **history** describes a list of OncoTree codes which used to be called `X` but refers to the same set of cases that it currently refers to. Synonyms can be automatically converted in both directions (i.e., between older and later release versions of the OncoTree).

***

A **revocation** represents OncoTree codes for which the current OncoTree node _was_ a part of the meaning of but the OncoTree codes have since been revoked from the current OncoTree release version. One such example of this type of case is shown in the Example 1 schema below.
