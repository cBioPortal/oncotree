# OncoTree History Modeling

### Background

The OncoTree is an evolving ontology and as such has gone through several iterations since its conception to improve the standardization of cancer type diagnoses from a clinical perspective. As such, some nodes on the OncoTree possess a complex revision history whereas others have a much more simple revision history, such as a simple renaming of the detailed cancer type name.

The complexity of the revision history for some OncoTree nodes created a need for a tool that facilitates the mapping of OncoTree codes between different OncoTree release versions.

### OncoTree history modeling

Three new properties were introduced as a result of the OncoTree history modeling. Each node will now contain the following new properties and will be discussed in further detail below.

**1. History:** A list of _synonymous_  OncoTree codes
**2. Revocations:** A list of OncoTree codes which the current node _was_ a part of the meaning of but have since been revoked from subsequent OncoTree release versions
**3. Precursors:** A list of OncoTree codes which _became_ a part of the meaning of the current node

***

The **history** describes a list of OncoTree codes which used to be called `X` but refers to the same set of cases that it currently refers to. Synonyms can be automatically converted in both directions (i.e., between older and later release versions of the OncoTree).

***

A **revocation** represents OncoTree codes for which the current OncoTree node _was_ a part of the meaning of but the OncoTree codes have since been revoked from the current OncoTree release version. One such example of this type of case is shown in the Example 1 schema below.
