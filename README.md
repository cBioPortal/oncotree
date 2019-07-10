# OncoTree

## General

The OncoTree is an open-source ontology that was developed at [Memorial Sloan Kettering Cancer Center](https://www.mskcc.org/) (MSK) for standardizing cancer type diagnosis from a clinical perspective by assigning each diagnosis a unique OncoTree code.

OncoTree codes are linked to every study imported into the [cBioPortal for Cancer Genomics](https://www.cbioportal.org/), where each sample is annotated with its own OncoTree code corresponding to its respective cancer type.

These codes are also used by [OncoKB](http://oncokb.org/), a precision oncology knowledge base developed at MSK containing information about the effects and treatment implications of specific cancer gene alterations.


Read about our latest developments on our [News page](/docs/News.md).

Users may submit their OncoTree related questions to the [OncoTree Users Google Group](https://groups.google.com/forum/#!forum/oncotree-users).

## OncoTree Mapping Tool

The OncoTree Mapping Tool was developed to facilitate the mapping of OncoTree codes between different OncoTree release versions. To learn more about the OncoTree Converter tool, please refer to the [OncoTree Mapping documentation](/docs/OncoTree-Mapping-Tool.md).

## OncoTree History Modeling
The OncoTree is an evolving ontology and as such has gone through several iterations since its conception to improve the standardization of cancer type diagnoses from a clinical perspective. As such, some nodes on the OncoTree possess a complex revision history whereas others have a much more simple revision history.

## OncoTree API

For APIs, please see the [OncoTree Swagger page](http://oncotree.mskcc.org/#/home?tab=api)

## Start a frontend only website
### Install/Start project
1. Install npm globally
2. Go to web/src/main/resources/
3. Run `npm install`
4. Run `grunt serve`

## License
<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
