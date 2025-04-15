### News
#### April 8, 2025
 *   **New Stable Release** OncoTree version *oncotree_2025_04_08* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2021_11_02*.
*   **New nodes added:**
     * NUTM1 Renal Cell Carcinoma (NRCC)
     * Adenocarcinoma in Retrorectal Cystic Hamartoma (ARCH)
     * Xanthogranuloma (XGA)
     * Three new nodes (NVRINT, MPNWP, MDSWP) initially intended only oncotree_candidate_release were added to this release.
 *   **New mappings added** OncoTree now includes expanded NCI and UMLS mappings, improving coverage for previously missing cancer classifications.    
 #### November 2, 2021
 *   **New Stable Release** OncoTree version *oncotree_2021_11_02* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2020_10_01*.
 *   **New nodes added:**
     * Desmoplastic/Nodular Medulloblastoma, NOS (DMBLNOS)
     * Desmoplastic/Nodular Medulloblastoma, SHH Subtype (DMBLSHH)
     * Anaplastic Medulloblastoma, NOS (AMBLNOS)
     * Anaplastic Medulloblastoma, Non-WNT, Non-SHH (AMBLNWS)
     * Anaplastic Medulloblastoma, Group 3 (AMBLNWSG3)
     * Anaplastic Medulloblastoma, Group 4 (AMBLNWSG4)
     * Anaplastic Medulloblastoma, SHH Subtype (AMBLSHH)
     * Medulloblastoma, NOS (MBLNOS)
     * Medulloblastoma, Non-WNT, Non-SHH (MBLNWS)
     * Medulloblastoma, Group 3 (MBLG3)
     * Medulloblastoma, Group 4 (MBLG4)
     * Medulloblastoma, SHH Subtype (MBLSHH)
     * Medulloblastoma, WNT Subtype (MBLWNT)
     * Medulloblastoma with Extensive Nodularity, NOS (MBENNOS)
     * Medulloblastoma with Extensive Nodularity, SHH Subtype (MBENSHH)
     * Pancreatic Neuroendocrine Carcinoma (PANEC) *in the original version of this news release this node was accidentally omitted*
 *   **Nodes reclassified**
     * The following list of oncotree codes had the mainType shortened to drop the suffix "NOS": ADRENAL_GLAND, AMPULLA_OF_VATER, BILIARY_TRACT, BLADDER, BONE, BOWEL, BRAIN, BREAST, CERVIX, EYE, HEAD_NECK, KIDNEY, LIVER, LUNG, LYMPH, MYELOID, OTHER, OVARY, PANCREAS, PENIS, PERITONEUM, PLEURA, PNS, PROSTATE, SKIN, SOFT_TISSUE, STOMACH, TESTIS, THYMUS, THYROID, UTERUS, VULVA
     * Cholangiocarcinoma (CHOL) now has direct parent Intraductal Papillary Neoplasm of the Bile Duct (IPN) [previously: Biliary Tract (BILIARY_TRACT)]
     * Gallbladder Cancer (GBC) now has direct parent Intracholecystic Papillary Neoplasm (ICPN) [previously: Biliary Tract (BILIARY_TRACT)]
 *   **Oncotree candidate release version additions:**
     * Three new nodes intended only for version oncotree_candidate_release were added (NVRINT, MPNWP, MDSWP). These nodes will not be incorperated into oncotree latest stable.
 *   **Resources added**
     * rdf formatted OWL ontology and taxomomy files for recent oncotree versions have been added to our github repository. They can be explored [here <span class="text-primary oi oi-external-link"></span>](https://github.com/cBioPortal/oncotree/tree/master/resources/rdf).
#### November 4, 2020
 *   **Ontology to Ontology Mapping tool available**
     * The Ontology Mapping tool was developed to facilitate the mapping between different cancer classification systems. We currently support mapping between OncoTree, ICD-O, NCIt, UMLS and HemeOnc systems.
     * A mapping file containing mappings between the different ontologies and OncoTree codes as well as a python script to run the mapping is available on the OncoTree [GitHub page <span class="text-primary oi oi-external-link"></span>](https://github.com/cBioPortal/oncotree/tree/master/scripts/ontology_to_ontology_mapping_tool). Details are also available on the [Mapping Tools page <span class="text-primary oi oi-external-link"></span>](http://oncotree.info/images/../#/home?tab=mapping).
     * The mapping file is expected to grow as we curate more ontology mappings for OncoTree codes.
#### October 1, 2020
 *   **New Stable Release** OncoTree version *oncotree_2020_10_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2020_04_01*.
 *   **New nodes added:**
     * Basal Cell Carcinoma of Prostate (BCCP)
 *   **Node deleted:**
     * Porphyria Cutania Tarda (PCT)
 *   **Node with updated name:**
     * Goblet Cell Adenocarcinoma of the Appendix (GCCAP) [previously: Goblet Cell Carcinoid of the Appendix (GCCAP)].
#### April 1, 2020
 *   **New Stable Release** OncoTree version *oncotree_2020_04_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2020_02_06*.
 *   **New nodes added:**
     * AML with Variant RARA translocation (AMLRARA)
 *   **Nodes reclassified:**
     * Mixed Cancer Types (MIXED is now a child of Other (OTHER) [previously under: Cancer of Unknown Primary (CUP)].
#### February 6, 2020
 *   **Web Link To Specific Nodes Available:**
     * When providing web links to oncotree, you may include a search term as an optional argument and the oncotree you reference will be displayed with the search term located and highlighted. Use of this mechanism can be seen in the [example output <span class="text-primary oi oi-external-link"></span>](https://raw.githubusercontent.com/cBioPortal/oncotree/master/docs/resources/data_clinical_sample_remapped_summary.html) for the OncoTree-to-OncoTree code mapping tool.
     * As an example, you can provide a direct link to the TumorType node PANET in oncotree version oncotree_2019_12_01 with this link: `http://oncotree.mskcc.org/#/home?version=oncotree_2019_12_01&search_term=(PANET)`
 *   **New Stable Release** OncoTree version *oncotree_2020_02_06* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2020_02_01*.
 *   **Nodes reclassified:**
     * Gallbladder Cancer (GBC) is now a child of Biliary Tract (BILIARY_TRACT) [previously under: Intracholecystic Papillary Neoplasm (ICPN)].
     * Cholangiocarcinoma (CHOL) is now a child of Biliary Tract (BILIARY_TRACT) [previously under: Intraductal Papillary Neoplasm of the Bile Duct (IPN)].
#### February 1, 2020
 *   **New Stable Release** OncoTree version *oncotree_2020_02_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2019_12_01*.
 *   **New nodes added:**
     * Intraductal Oncocytic Papillary Neoplasm (IOPN)
     * Intraductal Tubulopapillary Neoplasm (ITPN)
     * Intracholecystic Papillary Neoplasm (ICPN)
     * Intraductal Papillary Neoplasm of the Bile Duct (IPN)
 *   **Nodes reclassified:**
     * Gallbladder Cancer (GBC) is now a child of Intracholecystic Papillary Neoplasm (ICPN) [previously under: Biliary Tract (BILIARY_TRACT)].
     * Cholangiocarcinoma (CHOL) is now a child of Intraductal Papillary Neoplasm of the Bile Duct (IPN) [previously under: Biliary Tract (BILIARY_TRACT)].
#### December 1, 2019
 *   **New Stable Release** OncoTree version *oncotree_2019_12_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2019_08_01*.
 *   **New nodes added:**
     * Low-grade Appendiceal Mucinous Neoplasm (LAMN)
#### August 1, 2019
 *   **New Stable Release** OncoTree version *oncotree_2019_08_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2019_05_01*.
 *   **New nodes added:**
     * Lacrimal Gland Tumor (LGT)
     * Adenoid Cystic Carcinoma of the Lacrimal Gland (ACLG)
     * Squamous Cell Carcinoma of the Lacrimal Gland (SCLG)
     * Basal Cell Adenocarcinoma (BCAC)
     * Carcinoma ex Pleomorphic Adenoma (CAEXPA)
     * Pleomorphic Adenoma (PADA)
     * Polymorphous Adenocarcinoma (PAC)
     * Atypical Lipomatous Tumor (ALT)
#### May 2, 2019
 *   **OncoTree-to-OncoTree code mapping tool updated**
     * The OncoTree-to-OncoTree mapping tool (now version 1.2) has been updated:
         * it no longer requires the installation of the python 'requests' module
         * it now is able to handle input files where lines end in carriage return (such as files saved from Microsoft Excel)
#### May 1, 2019
 *   **New Stable Release** OncoTree version *oncotree_2019_05_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2019_03_01*.
 *   **Nodes reclassified**
     * Intestinal Ampullary Carcinoma (IAMPCA) MainType is now Ampullary Cancer [previously: Ampullary Carcinoma]
     * Mixed Ampullary Carcinoma (MAMPCA) MainType is now Ampullary Cancer [previously: Ampullary Carcinoma]
     * Pancreatobiliary Ampullary Carcinoma (PAMPCA) MainType is now Ampullary Cancer [previously: Ampullary Carcinoma]
     * Ependymoma (EPM) MainType is now Glioma [previously: CNS Cancer]
     * Rosai-Dorfman Disease (RDD) MainType is now Histiocytosis [previously: Histiocytic Disorder]
#### March 26, 2019
 *   **OncoTree-to-OncoTree code mapping tool updated**
     * The OncoTree-to-OncoTree mapping tool (now version 1.1) has been slightly improved, mainly by adding a separate section in the output summary for reporting OncoTree codes which were replaced automatically. Also, additional documentation and description of the tool has been added under the "Mapping Tools" tab.
#### March 14, 2019
 *   **OncoTree-to-OncoTree code mapping tool available**
     * A python program is now available for download under the webpage tab labeled "Mapping Tools". This tool will rewrite OncoTree codes in a tabular clinical data file, mapping from one version of OncoTree to another. When additional guidance is necessary, the program will insert a column containing options and comments next to the ONCOTREE_CODE column. After selecting an appropriate OncoTree code for cases which require action, this extra column can be deleted to produce a fully re-mapped clinical data file. More details about this tool and how to use it are available under the "Mapping Tools" tab.
     * The OncoTree-to-OncoTree mapping tool relies on an expanded model of OncoTree node history. This is reflected in the Web API schema for Tumor Types, which now has added properties called "precursors" and "revocations". Using these properties, and the existing "history" property, the tool is able to make proper mappings across OncoTree versions, and give suggestions when no clear mapping is available. It is also reflected in the main tree visualization of OncoTree, where a combination of these properties (when set) will be displayed in the pop-up information box with label "Previous Codes".
#### March 1, 2019
 *   **New Stable Release** OncoTree version *oncotree_2019_03_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2019_02_01*.
 *   **New node added:**
     * NUT Carcinoma of the Lung (NUTCL)
 *   **Node with renamed OncoTree code:**
     * Monomorphic Epitheliotropic Intestinal T-Cell Lymphoma (MEITL) [previously: MEATL]
 *   **Node with renamed OncoTree code and updated name:**
     * Germ Cell Tumor with Somatic-Type Malignancy (GCTSTM) [previously: Teratoma with Malignant Transformation (TMT)]
 *   **Cross-version OncoTree code mapping tool coming soon**
     * Development of a tool to map OncoTree codes between different versions of OncoTree is nearing completion.
     * <span class="oi oi-warning text-danger" aria-hidden="true"></span> Related to this, users of the OncoTree Web API should be aware that we will soon be adding two additional properties to the output schema returned by the api/tumorTypes endpoints. The properties "precursors" and "revocations" will be added alongside the "history" property (having the same type: an array of strings). These will help distinguish the kinds of possible relationships to OncoTree nodes in prior versions of OncoTree. We expect this new schema to be backwards compatible, but if your language or tools requires an exactly matching JSON schema you will need to make adjustments.
#### February 1, 2019
 *   **New Stable Release** OncoTree version *oncotree_2019_02_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2018_11_01*.
 *   **New node added:**
     * Leiomyoma (LM)
#### November 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_11_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2018_09_01*.
 *   **New nodes added:**
     * Gastrointestinal Neuroendocrine Tumors of the Esophagus/Stomach (GINETES)
     * High-Grade Neuroendocrine Carcinoma of the Esophagus (HGNEE)
     * High-Grade Neuroendocrine Carcinoma of the Stomach (HGNES)
 *   **Nodes reclassified:**
     * Well-Differentiated Neuroendocrine Tumors of the Stomach (SWDNET) is now a child of Gastrointestinal Neuroendocrine Tumors of the Esophagus/Stomach (GINETES) [previously under: Bowel (BOWEL)].
#### September 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_09_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2018_08_01*.
 *   **Nodes with adjusted History:**
     * Myeloid Neoplasm (MNM) previous code: LEUK
     * B-Lymphoblastic Leukemia/Lymphoma (BLL) previous code: BALL
     * T-Lymphoblastic Leukemia/Lymphoma (TLL) previous code: TALL
     * Essential Thrombocythemia (ET) previous code: ETC
     * Polycythemia Vera (PV) previous code: PCV
     * Diffuse Large B-Cell Lymphoma, NOS (DLBCLNOS) previous code: DLBCL
     * Sezary Syndrome (SS) previous code: SEZS
#### August 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_08_01* is now the latest stable release. The previous stable version is still accessible as version *oncotree_2018_07_01*.
 *   **New nodes added:**
     * Angiomatoid Fibrous Histiocytoma (AFH)
     * Clear Cell Sarcoma of Kidney (CCSK)
     * Ewing Sarcoma of Soft Tissue (ESST)
     * Extra Gonadal Germ Cell Tumor (EGCT)
     * Infantile Fibrosarcoma (IFS)
     * Malignant Glomus Tumor (MGST)
     * Malignant Rhabdoid Tumor of the Liver (MRTL)
     * Myofibromatosis (IMS)
     * Sialoblastoma (SBL)
     * Undifferentiated Embryonal Sarcoma of the Liver (UESL)
#### July 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_07_01* is now the latest stable release. The previous stable version is still accessible through the OncoTree web API by requesting version *oncotree_2018_06_15*.
 *   **Node with renamed OncoTree code:**
     * Atypical Chronic Myeloid Leukemia, BCR-ABL1- (ACML) [previously: aCML]
#### June 15, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_06_15* is now the latest stable release. The previous stable version is still accessible through the OncoTree web API by requesting version *oncotree_2018_06_01*.
 *   **Nodes reclassified**
     * Mature B-Cell Neoplasms (MBN) and Mature T and NK Neoplasms (MTNN) and their subnodes were relocated to be under Non-Hodgkin Lymphoma (NHL).
     * Rosai-Dorfman Disease (RDD) was relocated to be under Histiocytic and Dendritic Cell Neoplasms (HDCN). Previously RDD was under Lymphoid Neoplasm (LNM) in the Lymphoid category.
#### June 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_06_01* is now the latest stable release. The previous stable version is still accessible through the OncoTree web API by requesting version *oncotree_2018_05_01*.
 *   **Blood and Lymph subtrees replaced with new Myeloid and Lymphoid subtrees:**
     * 23 nodes in the previous Blood subtree have been deleted or relocated/renamed. Affected OncoTree codes from this tree: BLOOD, BPDCN, HIST, LCH, ECD, LEUK, ALL, BALL, TALL, AMOL, AML, CLL, CML, CMML, HCL, LGLL, MM, MDS, MPN, ETC, MYF, PCV, SM
     * 28 nodes in the previous Lymph subtree have been deleted or relocated/renamed. Affected OncoTree codes from this tree: LYMPH, HL, CHL, NLPHL, NHL, BCL, BL, DLBCL, MALTL, FL, MCL, MZL, MBCL, NMZL, PCNSL, PEL, SLL, SMZL, WM, TNKL, CTCL, MYCF, SEZS, PTCL, ALCL, AITL, PTCLNOS, RD
     * 122 nodes in the current Lymphoid subtree have been added or relocated/renamed from previous subtrees. OncoTree codes in this new subtree are: LYMPH, LATL, LBGN, LNM, BLL, BLLRGA, BLLHYPER, BLLHYPO, BLLIAMP21, BLLETV6RUNX1, BLLTCF3PBX1, BLLIL3IGH, BLLBCRABL1, BLLKMT2A, BLLBCRABL1L, BLLNOS, HL, CHL, LDCHL, LRCHL, MCCHL, NSCHL, NLPHL, MBN, ALKLBCL, AHCD, BCLU, BPLL, BL, BLL11Q, CLLSLL, DLBCLCI, DLBCLNOS, ABC, GCB, EBVDLBCLNOS, EBVMCU, EP, FL, DFL, ISFN, GHCD, HHV8DLBCL, HCL, HGBCL, HGBCLMYCBCL2, IVBCL, LBLIRF4, LYG, LPL, WM, MCL, ISMCL, MZL, EMALT, NMZL, SMZL, MCBCL, MGUS, MGUSIGA, MGUSIGG, MGUSIGM, MIDD, MIDDA, MIDDO, MHCD, PTFL, PCM, PLBL, PCLBCLLT, PCFCL, PCNSL, PEL, PMBL, SPB, SBLU, HCL-V, SDRPL, THRLBCL, MTNN, ATLL, ANKL, ALCL, ALCLALKN, ALCLALKP, BIALCL, AITL, CLPDNK, EATL, ENKL, FTCL, HSTCL, HVLL, ITLPDGI, MEATL, MYCF, NPTLTFH, PTCL, PCATCL, PCLPD, LYP, PCALCL, PCSMTPLD, PCAECTCL, PCGDTCL, SS, SPTCL, SEBVTLC, TLGL, TPLL, NHL, PTLD, CHLPTLD, FHPTLD, IMPTLD, MPTLD, PHPTLD, PPTLD, RDD, TLL, ETPLL, NKCLL
     * 101 nodes in the current Myeloid subtree have been added or relocated/renamed from previous subtrees. OncoTree codes in this new subtree are: MYELOID, MATPL, MBGN, MNM, ALAL, AUL, MPALBCRABL1, MPALKMT2A, MPALBNOS, MPALTNOS, AML, AMLMRC, AMLRGA, AMLRBM15MKL1, AMLBCRABL1, AMLCEBPA, AMLNPM1, AMLRUNX1, AMLCBFBMYH11, AMLGATA2MECOM, AMLDEKNUP214, AMLRUNX1RUNX1T1, AMLMLLT3KMT2A, APLPMLRARA, AMLNOS, AM, AMLMD, AWM, ABL, AMKL, AMOL, AMML, APMF, PERL, MPRDS, MLADS, TAM, MS, TMN, TAML, TMDS, BPDCN, HDCN, JXG, ECD, FRCT, FDCS, HS, IDCT, IDCS, LCH, LCS, MCD, CMCD, MCSL, SM, ASM, ISM, SMMCL, SSM, SMAHN, MDS, MDSEB, MDSEB1, MDSEB2, MDSID5Q, MDSMD, MDSRS, MDSRSMD, MDSRSSLD, MDSSLD, MDSU, RCYC, MDS/MPN, aCML, CMML, CMML0, CMML1, CMML2, JMML, MDSMPNRST, MDSMPNU, MNGLP, MLNER, MLNFGFR1, MLNPCM1JAK2, MLNPDGFRA, MLNPDGFRB, MPN, CELNOS, CML, CMLBCRABL1, CNL, ET, ETMF, MPNU, PV, PVMF, PMF, PMFPES, PMFOFS
 *   **Nodes reclassified**
     * Adrenocortical Adenoma (ACA) MainType is now Adrenocortical Adenoma [previously: Adrenocortical Carcinoma]
     * Ampulla of Vater (AMPULLA_OF_VATER) MainType is now Ampullary Cancer [previously: Ampullary Carcinoma]
     * Parathyroid Cancer (PTH) MainType is now Parathyroid Cancer [previously: Head and Neck Cancer]
     * Parathyroid Carcinoma (PTHC) MainType is now Parathyroid Cancer [previously: Head and Neck Cancer]
     * Follicular Dendritic Cell Sarcoma (FDCS) was moved from the Soft Tissue subtree into the Myeloid subtree with direct parent Histiocytic and Dendritic Cell Neoplasms (HDCN)
     * Interdigitating Dendritic Cell Sarcoma (IDCS) was moved from the Soft Tissue subtree into the Myeloid subtree with direct parent Histiocytic and Dendritic Cell Neoplasms (HDCN)
 *   **New nodes added:**
     * Inverted Urothelial Papilloma (IUP)
     * Urothelial Papilloma (UPA)
     * Oncocytic Adenoma of the Thyroid (OAT)
 *   **Character set simplification**
     * Salivary Gland-Type Tumor of the Lung (SGTTL) used to contain a unicode character for a horizontal dash. This punctuation mark is now a hyphen.
#### May 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_05_01* is now the latest stable release. The previous stable version is still accessible through the OncoTree web API by requesting version *oncotree_2018_04_01*.
 *   **New nodes added:**
     * Primary CNS Melanocytic Tumors (PCNSMT)
     * Melanocytoma (MELC)
 *   **Node reclassified**
     * Primary CNS Melanoma (PCNSM) now is a child of Primary CNS Melanocytic Tumors (PCNSMT) [previously under: CNS/Brain].
#### April 23, 2018
 *   **New Web API Version Available**
     * A new version (v1.0.0) of the OncoTree Web API is available. It can be explored here:
http://oncotree.mskcc.org/swagger-ui.html <br> The previous version is still available, but is scheduled to be discontinued May 31, 2018
You can continue to access the previous version (v0.0.1) in its original location summarized here: ~~http://oncotree.mskcc.org/oncotree/swagger-ui.html~~
 *   **Details and Migration Guidance**
     * The base URL for accessing all API functionality is being simplified from ~~http://oncotree.mskcc.org/oncotree/~~ to http://oncotree.mskcc.org/
     * <span class="oi oi-warning text-danger" aria-hidden="true"></span> The /api/tumor_types.txt endpoint is now deprecated. It is scheduled for deletion as part of the next API version release.
     * Most endpoint paths in the API remain the same and provide the same services. Exceptions are:
         * /api/tumorTypes used to accept a query parameter ("flat") which controlled the output format for receiving a tree representation or a flat representation of the full set of TumorTypes. Now this endpoint always returns a flat list of all TumorTypes and a new endpoint path (/api/tumorTypes/tree) is used to retrieve a tree representation of the OncoTree. Previous requests which included "flat=false" should be adjusted to use the /api/tumorTypes/tree endpoint. Otherwise "flat=true" should be dropped from the request.
         * /api/tumorTypes used to accept a query parameter ("deprecated") which is no longer recognized. This parameter should be dropped from requests. Deprecated OncoTree codes can instead be found in the history attribute of the response.
         * the POST request endpoint (/api/tumorTypes/search) which accepted a list of TumorType queries has been deprecated and is no longer available through the swagger-ui interface. The GET request endpoint /api/tumorTypes/search/{type}/{query} remains available as before. If you previously submitted an array of query requests, you should iterate through the array and call the GET request endpoint to make one query per request.
     * The output format (schema) of many endpoints has been simplified. You will need to adjust your result handling accordingly. Changes include:
         * responses no longer include a "meta" element with associated code and error messages. Instead HTTP status codes are set appropriately and error messages are supplied in message bodies. Responses also no longer contain a "data" element. Objects representing the API output are directly returned instead.
         * MainType values are no longer modeled as objects. Each MainType value is now represented as a simple string. The /api/mainTypes endpoint now returns an array of strings rather than an object mapping MainType names to MainType objects.
         * TumorType values no longer contain elements "id", "deprecated", "links", "NCI", "UMLS". A new element ("externalReferences") has been added which contains a JSON object mapping external authority names to arrays of associated identifiers. Such as "externalReferences": {"UMLS": ["CL497188","C1510796"],"NCI": ["C123384","C40361"]}
     * Argument validation has been strengthened for several parameters, such as "type" and "levels" in the /api/tumorTypes/search/{type}/{query} endpoint. Now improper arguments cause an a HTTP status response indicating error, with a description of the problem in the body.
     * Some requests which fail to find matching entities now return NOT_FOUND HTTP status code 404 rather than an empty result. Examples: http://oncotree.mskcc.org/api/tumorTypes/search/code/TEST_UNDEFINED_CODE or http://oncotree.mskcc.org/api/crosswalk?vocabularyId=ICDO&conceptId=C15
#### April 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_04_01* is now the latest stable release. The previous stable version is still accessible through the OncoTree web API by requesting version *oncotree_2018_03_01*.
 *   **New nodes added:**
     * Adamantinoma (ADMA)
     * Tubular Adenoma of the Colon (TAC)
     * Parathyroid Cancer (PTH)
     * Parathyroid Carcinoma (PTHC)
     * Renal Neuroendocrine Tumor (RNET)
#### March 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_03_01* is now the latest stable release. The previous stable version is still accessible through the OncoTree web API by requesting version *oncotree_2018_02_01*.
 *   **New node added:**
     * Ganglioneuroma (GN)
 *   **Node reclassified**
     * Rhabdoid Cancer (MRT) now has direct parent Kidney (KIDNEY) [previously: Wilms' Tumor (WT)] Main type is now "Rhabdoid Cancer" [previously: Wilms Tumor]
#### February 7, 2018
 *   **OncoTree format expanded to support deeper tree nodes**
     * To support upcoming expansion of the OncoTree, the 5 named levels of the OncoTree {*Primary*, *Secondary*, *Tertiary*, *Quaternary*, *Quinternary*} have been dropped in favor of level numbers {1, 2, 3, 4, 5, ...}. Web API functions have been adjusted accordingly, and an API function which outputs a table format of the OncoTree has been adjusted to output 7 levels of depth.
 *   **Additional improvements to the website** :
     * tab-style navigation
     * more prominent version selection information
     * a new **News** page
#### February 1, 2018
 *   **New Stable Release** OncoTree version *oncotree_2018_02_01* is now the latest stable release. The previous stable version is still accessible for use through version name *oncotree_2018_01_01*.
 *   **New nodes added:**
     * Adenosquamous Carcinoma of the Gallbladder (GBASC)
     * Gallbladder Adenocarcinoma, NOS (GBAD)
     * Small Cell Gallbladder Carcinoma (SCGBC)
     * Juvenile Secretory Carcinoma of the Breast (JSCB)
     * Osteoclastic Giant Cell Tumor (OSGCT)
     * Peritoneal Serous Carcinoma (PSEC)
 *   **Nodes reclassified** [from: Embryonal Tumor, to: Peripheral Nervous System]:
     * Ganglioneuroblastoma (GNBL)
     * Neuroblastoma (NBL)
 *   **Node with renamed OncoTree code:**
     * Spindle Cell Carcinoma of the Lung (SPCC) [previously: SpCC]
