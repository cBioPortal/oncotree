### News
#### May 1, 2018
 *   **New Stable Release** Oncotree version *oncotree_2018_05_01* is now the latest stable release. The previous stable version is still accessible through the oncotree web API by requesting version *oncotree_2018_04_01*.
 *   **New nodes added:**
        * Primary CNS Melanocytic Tumors (PCNSMT)
        * Melanocytoma (MELC)
 *   **Node reclassified**
        * Primary CNS Melanoma (PCNSM) now is a child of Primary CNS Melanocytic Tumors (PCNSMT) [previously under: CNS/Brain].
#### April 23, 2018
 *   **New Web API Version Available**
     * A new version (v1.0.0) of the oncotree Web API is available. It can be explored here:
http://oncotree.mskcc.org/swagger-ui.html <br> The previous version is still available, but is scheduled to be discontinued May 31, 2018
You can continue to access the previous version (v0.0.1) in its original location summarized here: http://oncotree.mskcc.org/oncotree/swagger-ui.html
 *   **Details and Migration Guidance**
     * The base URL for accessing all API functionality is being simplified from http://oncotree.mskcc.org/oncotree/ to http://oncotree.mskcc.org/
     * <span class="glyphicon glyphicon-exclamation-sign alert-danger" aria-hidden="true"></span> The /api/tumor_types.txt endpoint is now deprecated. It is scheduled for deletion as part of the next API version release.
     * Most endpoint paths in the API remain the same and provide the same services. Exceptions are:
         * /api/tumorTypes used to accept a query parameter ("flat") which controlled the output format for receiving a tree representation or a flat representation of the full set of TumorTypes. Now this endpoint always returns a flat list of all TumorTypes and a new endpoint path (/api/tumorTypes/tree) is used to retrieve a tree representation of the oncotree. Previous requests which included "flat=false" should be adjusted to use the /api/tumorTypes/tree endpoint. Otherwise "flat=true" should be dropped from the request.
         * /api/tumorTypes used to accept a query parameter ("deprecated") which is no longer recognized. This parameter should be dropped from requests. Deprecated oncotree codes can instead be found in the history attribute of the response.
         * the POST request endpoint (/api/tumorTypes/search) which accepted a list of TumorType queries has been deprecated and is no longer available through the swagger-ui interface. The GET request endpoint /api/tumorTypes/search/{type}/{query} remains available as before. If you previously submitted an array of query requests, you should iterate through the array and call the GET request endpoint to make one query per request.
     * The output format (schema) of many endpoints has been simplified. You will need to adjust your result handling accordingly. Changes include:
         * responses no longer include a "meta" element with associated code and error messages. Instead HTTP status codes are set appropriately and error messages are supplied in message bodies. Responses also no longer contain a "data" element. Objects representing the API output are directly returned instead.
         * MainType values are no longer modeled as objects. Each MainType value is now represented as a simple string. The /api/mainTypes endpoint now returns an array of strings rather than an object mapping MainType names to MainType objects.
         * TumorType values no longer contain elements "id", "deprecated", "links", "NCI", "UMLS". A new element ("externalReferences") has been added which contains a JSON object mapping external authority names to arrays of associated identifiers. Such as "externalReferences": {"UMLS": ["CL497188","C1510796"],"NCI": ["C123384","C40361"]}
     * Argument validation has been strengthened for several parameters, such as "type" and "levels" in the /api/tumorTypes/search/{type}/{query} endpoint. Now improper arguments cause an a HTTP status response indicating error, with a description of the problem in the body.
     * Some requests which fail to find matching entities now return NOT_FOUND HTTP status code 404 rather than an empty result. Examples: http://oncotree.mskcc.org/oncotree_test/api/tumorTypes/search/code/TEST_UNDEFINED_CODE or http://oncotree.mskcc.org/oncotree_test/api/crosswalk?vocabularyId=ICDO&conceptId=C15
#### April 1, 2018
 *   **New Stable Release** Oncotree version *oncotree_2018_04_01* is now the latest stable release. The previous stable version is still accessible through the oncotree web API by requesting version *oncotree_2018_03_01*.
 *   **New nodes added:**
        * Adamantinoma (ADMA)
        * Tubular Adenoma of the Colon (TAC)
        * Parathyroid Cancer (PTH)
        * Parathyroid Carcinoma (PTHC)
        * Renal Neuroendocrine Tumor (RNET)
#### March 1, 2018
 *   **New Stable Release** Oncotree version *oncotree_2018_03_01* is now the latest stable release. The previous stable version is still accessible through the oncotree web API by requesting version *oncotree_2018_02_01*.
 *   **New node added:**
        * Ganglioneuroma (GN)
 *   **Node reclassified**
        * Rhabdoid Cancer (MRT) now has direct parent Kidney (KIDNEY) [previously: Wilms' Tumor (WT)] Main type is now "Rhabdoid Cancer" [previously: Wilms Tumor]
#### February 7, 2018
 *   **Oncotree format expanded to support deeper tree nodes**
        * To support upcoming expansion of the oncotree, the 5 named levels of the oncotree {*Primary*, *Secondary*, *Tertiary*, *Quaternary*, *Quinternary*} have been dropped in favor of level numbers {1, 2, 3, 4, 5, ...}. Web API functions have been adjusted accordingly, and an API function which outputs a table format of the oncotree has been adjusted to output 7 levels of depth.
 *   **Additional improvements to the website** :
        * tab-style navigation
        * more prominent version selection information
        * a new **News** page
#### February 1, 2018
 *   **New Stable Release** Oncotree version *oncotree_2018_02_01* is now the latest stable release. The previous stable version is still accessible for use through version name *oncotree_2018_01_01*.
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
 *   **Node with renamed oncotree code:**
        * Spindle Cell Carcinoma of the Lung (SPCC) [previously: SpCC]
