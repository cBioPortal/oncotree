import{_ as e,c as t,o,a3 as n}from"./chunks/framework.CyEiTwkJ.js";const y=JSON.parse('{"title":"Frequently Asked Questions (FAQ)","description":"","frontmatter":{},"headers":[],"relativePath":"faq.md","filePath":"faq.md"}'),s={name:"faq.md"},i=n('<h1 id="frequently-asked-questions-faq" tabindex="-1">Frequently Asked Questions (FAQ) <a class="header-anchor" href="#frequently-asked-questions-faq" aria-label="Permalink to &quot;Frequently Asked Questions (FAQ)&quot;">​</a></h1><h2 id="_1-why-do-you-distinguish-between-detailed-tumor-types-and-main-tumor-types-and-why-are-the-main-tumor-types-not-entered-into-the-oncotree-with-separate-nodes" tabindex="-1">1. Why do you distinguish between “detailed tumor types” and “main tumor types,” and why are the main tumor types not entered into the OncoTree with separate nodes? <a class="header-anchor" href="#_1-why-do-you-distinguish-between-detailed-tumor-types-and-main-tumor-types-and-why-are-the-main-tumor-types-not-entered-into-the-oncotree-with-separate-nodes" aria-label="Permalink to &quot;1. Why do you distinguish between “detailed tumor types” and “main tumor types,” and why are the main tumor types not entered into the OncoTree with separate nodes?&quot;">​</a></h2><p>OncoTree distinguishes between <strong>detailed tumor types</strong> and <strong>main tumor types</strong> to maintain a structured hierarchy of cancer classifications. The <strong>detailed tumor types</strong> provide specific subcategories within each <strong>main tumor type</strong>, allowing for more precise annotation and analysis. The <strong>main tumor types</strong> act as broader categories that group together related detailed tumor types.</p><p>The reason main tumor types are not assigned separate nodes in OncoTree is that the classification is designed to focus on specific, well-defined tumor subtypes rather than broad categories. This ensures that OncoTree remains granular and clinically relevant for precision medicine applications.</p><h2 id="_2-do-you-have-codes-for-the-main-tumor-types-just-like-for-the-detailed-tumor-types-or-are-you-planning-on-assigning-them-in-the-future" tabindex="-1">2. Do you have codes for the main tumor types, just like for the detailed tumor types? Or are you planning on assigning them in the future? <a class="header-anchor" href="#_2-do-you-have-codes-for-the-main-tumor-types-just-like-for-the-detailed-tumor-types-or-are-you-planning-on-assigning-them-in-the-future" aria-label="Permalink to &quot;2. Do you have codes for the main tumor types, just like for the detailed tumor types? Or are you planning on assigning them in the future?&quot;">​</a></h2><p>Currently, OncoTree provides unique codes primarily for <strong>detailed tumor types</strong> to facilitate precise classification. While main tumor types do not have distinct codes assigned in the same way, efforts to develop mappings or codes for these broad categories may be considered in the future to improve interoperability with other classification systems.</p><h2 id="_3-why-do-you-include-so-many-main-tumor-types-with-the-extension-nos-these-types-are-not-included-in-the-drop-down-menu-for-the-tumor-types-on-the-oncokb-website-would-it-make-a-difference-to-the-results-when-using-these-nos-terms-as-input-to-the-web-api" tabindex="-1">3. Why do you include so many main tumor types with the extension “, NOS”? These types are not included in the drop-down menu for the tumor types on the OncoKB website. Would it make a difference to the results when using these NOS terms as input to the web API? <a class="header-anchor" href="#_3-why-do-you-include-so-many-main-tumor-types-with-the-extension-nos-these-types-are-not-included-in-the-drop-down-menu-for-the-tumor-types-on-the-oncokb-website-would-it-make-a-difference-to-the-results-when-using-these-nos-terms-as-input-to-the-web-api" aria-label="Permalink to &quot;3. Why do you include so many main tumor types with the extension “, NOS”? These types are not included in the drop-down menu for the tumor types on the OncoKB website. Would it make a difference to the results when using these NOS terms as input to the web API?&quot;">​</a></h2><p>The extension <strong>&quot;NOS&quot; (Not Otherwise Specified)</strong> is used to indicate tumor types that do not have a more specific classification. These are included to ensure compatibility with clinical terminology and pathology reports, which often use NOS when further molecular or histopathological details are unavailable.</p><p>Since OncoKB is optimized for precision oncology, the <strong>NOS terms</strong> may not appear in the drop-down menu because they are considered less specific. However, using NOS terms as input to the <strong>OncoKB web API</strong> could still yield results, depending on whether the system can map them to a more specific classification.</p><h2 id="_4-do-you-have-a-mapping-of-the-main-tumor-types-onto-nci-or-umls-as-for-the-detailed-tumor-types" tabindex="-1">4. Do you have a mapping of the main tumor types onto NCI or UMLS as for the detailed tumor types? <a class="header-anchor" href="#_4-do-you-have-a-mapping-of-the-main-tumor-types-onto-nci-or-umls-as-for-the-detailed-tumor-types" aria-label="Permalink to &quot;4. Do you have a mapping of the main tumor types onto NCI or UMLS as for the detailed tumor types?&quot;">​</a></h2><p>Yes, OncoTree provides <strong>detailed tumor type mappings</strong> to external ontologies such as:</p><ul><li><strong>NCI Thesaurus (NCIt)</strong></li><li><strong>Unified Medical Language System (UMLS)</strong></li></ul><p>However, mappings for <strong>main tumor types</strong> may not be explicitly provided. Users may need to rely on hierarchical relationships within OncoTree or external classification resources to infer these mappings.</p><h2 id="_5-do-you-have-a-mapping-for-the-tumor-types-onto-the-common-snomed-or-icd-10-codes-or-do-you-have-a-suggestion-on-how-to-perform-this-mapping" tabindex="-1">5. Do you have a mapping for the tumor types onto the common SNOMED or ICD-10 codes? Or do you have a suggestion on how to perform this mapping? <a class="header-anchor" href="#_5-do-you-have-a-mapping-for-the-tumor-types-onto-the-common-snomed-or-icd-10-codes-or-do-you-have-a-suggestion-on-how-to-perform-this-mapping" aria-label="Permalink to &quot;5. Do you have a mapping for the tumor types onto the common SNOMED or ICD-10 codes? Or do you have a suggestion on how to perform this mapping?&quot;">​</a></h2><p>OncoTree does not currently provide <strong>direct mappings to SNOMED CT or ICD-10</strong>. However, mappings can be established using:</p><ul><li><strong>NCI Thesaurus (NCIt)</strong>, which provides cross-references to SNOMED and ICD-10.</li><li><strong>UMLS Metathesaurus</strong>, which links medical terms across multiple ontologies, including SNOMED and ICD-10.</li></ul><p>To perform this mapping, you can:</p><ol><li>Use <strong>NCIt codes</strong> from OncoTree and find their corresponding entries in SNOMED or ICD-10.</li><li>Use the <strong>UMLS Metathesaurus API</strong> to look up terms and find equivalent SNOMED/ICD-10 codes.</li><li>Leverage tools like the <strong>National Library of Medicine (NLM) mappings</strong> to find equivalent ICD-10 codes.</li></ol>',18),a=[i];function r(h,d,u,p,m,l){return o(),t("div",null,a)}const g=e(s,[["render",r]]);export{y as __pageData,g as default};
