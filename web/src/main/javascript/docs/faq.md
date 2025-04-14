# Frequently Asked Questions (FAQ)

## 1. Why do you distinguish between “detailed tumor types” and “main tumor types,” and why are the main tumor types not entered into the OncoTree with separate nodes?

OncoTree distinguishes between **detailed tumor types** and **main tumor types** to maintain a structured hierarchy of cancer classifications. The **detailed tumor types** provide specific subcategories within each **main tumor type**, allowing for more precise annotation and analysis. The **main tumor types** act as broader categories that group together related detailed tumor types.

The reason main tumor types are not assigned separate nodes in OncoTree is that the classification is designed to focus on specific, well-defined tumor subtypes rather than broad categories. This ensures that OncoTree remains granular and clinically relevant for precision medicine applications.

## 2. Do you have codes for the main tumor types, just like for the detailed tumor types? Or are you planning on assigning them in the future?

Currently, OncoTree provides unique codes primarily for **detailed tumor types** to facilitate precise classification. While main tumor types do not have distinct codes assigned in the same way, efforts to develop mappings or codes for these broad categories may be considered in the future to improve interoperability with other classification systems.

## 3. Why do you include so many main tumor types with the extension “, NOS”? These types are not included in the drop-down menu for the tumor types on the OncoKB website. Would it make a difference to the results when using these NOS terms as input to the web API?

The extension **"NOS" (Not Otherwise Specified)** is used to indicate tumor types that do not have a more specific classification. These are included to ensure compatibility with clinical terminology and pathology reports, which often use NOS when further molecular or histopathological details are unavailable.

Since OncoKB is optimized for precision oncology, the **NOS terms** may not appear in the drop-down menu because they are considered less specific. However, using NOS terms as input to the **OncoKB web API** could still yield results, depending on whether the system can map them to a more specific classification.

## 4. Do you have a mapping of the main tumor types onto NCI or UMLS as for the detailed tumor types?

Yes, OncoTree provides **detailed tumor type mappings** to external ontologies such as:

- **NCI Thesaurus (NCIt)**
- **Unified Medical Language System (UMLS)**

However, mappings for **main tumor types** may not be explicitly provided. Users may need to rely on hierarchical relationships within OncoTree or external classification resources to infer these mappings.

## 5. Do you have a mapping for the tumor types onto the common SNOMED or ICD-10 codes? Or do you have a suggestion on how to perform this mapping?

OncoTree does not currently provide **direct mappings to SNOMED CT or ICD-10**. However, mappings can be established using:

- **NCI Thesaurus (NCIt)**, which provides cross-references to SNOMED and ICD-10.
- **UMLS Metathesaurus**, which links medical terms across multiple ontologies, including SNOMED and ICD-10.

To perform this mapping, you can:

1. Use **NCIt codes** from OncoTree and find their corresponding entries in SNOMED or ICD-10.
2. Use the **UMLS Metathesaurus API** to look up terms and find equivalent SNOMED/ICD-10 codes.
3. Leverage tools like the **National Library of Medicine (NLM) mappings** to find equivalent ICD-10 codes.
