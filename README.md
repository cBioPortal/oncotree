# OncoTree

## General

The OncoTree is an open-source ontology that was developed at [Memorial Sloan Kettering Cancer Center](https://www.mskcc.org/) (MSK) for standardizing cancer type diagnosis from a clinical perspective by assigning each diagnosis a unique OncoTree code.

OncoTree codes are linked to every study imported into the [cBioPortal for Cancer Genomics](https://www.cbioportal.org/), where each sample is annotated with its own OncoTree code corresponding to its respective cancer type.

These codes are also used by [OncoKB](http://oncokb.org/), a precision oncology knowledge base developed at MSK containing information about the effects and treatment implications of specific cancer gene alterations.


Read about our latest developments on our [News page](/docs/News.md).

Users may submit their OncoTree related questions to the [OncoTree Users Google Group](https://groups.google.com/forum/#!forum/oncotree-users).

## Annotation overlay

The tree can be overlaid with custom annotations that map OncoTree codes to
values. Each annotation may be a number, a text label, a gene list, or an
object combining them, e.g.:

```json
{
  "LUAD": { "label": "1204 samples", "value": 1204 },
  "GB": { "genes": ["EGFR", "PTEN", "TP53"] }
}
```

Each annotated node gets a small badge showing its value or gene count;
hovering the node adds the full detail (value, gene list) to the node's
tooltip. Collapsing a node rolls up its hidden descendants — values are summed
and gene lists unioned.

Annotations can be supplied four ways:

1. **Paste or upload** JSON/CSV in the *Annotations* panel.
2. **URL parameter** — `?annotations=<json>` where the value is raw
   (URL-encoded) JSON or base64-encoded JSON (the panel's "Copy share link"
   button produces the base64 form).
3. **Embed via `postMessage`** — add `?embed` to the URL to hide the site
   header and footer (leaving just the tree), then drive it from the host.
   When the app runs in an `<iframe>` it posts `{ type: "oncotree-ready" }` to
   its parent; the parent then pushes annotations:

   ```js
   iframe.contentWindow.postMessage(
     { type: "oncotree-annotations", annotations: { LUAD: 1204 } },
     "*",
   );
   ```

   The `annotations` payload may be an object, a JSON string, or `null` to
   clear.

   The parent can also drive the search, filtering the tree to the matching
   node(s) (by code, name, or annotation content — gene/label/value):

   ```js
   iframe.contentWindow.postMessage(
     { type: "oncotree-search", query: "EGFR" },
     "*",
   );
   ```

   Send an empty `query` (or `{ clear: true }`) to reset. The app posts back
   `{ type: "oncotree-search-result", query, count }` with the number of
   matches.

## Frontend Development

All of the frontend code can be found at [/web/src/main/javascript](/web/src/main/javascript). The only configuration needed is to set `ONCOTREE_BASE_URL` 
in [constants.ts](/web/src/main/javascript/src/shared/constants.ts). During development, it may be easiest to simply point to the public instance of 
[OncoTree](https://oncotree.mskcc.org).

Make sure you are using node version >=20.12.2. The frontend uses [pnpm](https://pnpm.io/) as its package manager (enable it with `corepack enable`).

To begin development run:
```
cd ./web/src/main/javascript
pnpm install && pnpm run dev
```

## Building the Frontend

The frontend must be transpiled to static assets before bundling into a jar. To do this follow the following steps:

1. Ensure that the correct `ONCOTREE_BASE_URL` is specified in [constants.ts](/web/src/main/javascript/src/shared/constants.ts).
2. Run the following:

    ```
    cd ./web/src/main/javascript
    pnpm install && pnpm run build
    ```
3. The frontend assets are now up to date, and you are ready to bundle the jar.

## Backend Development
All backend Go code can be found under /web/src/main/go. The backend provides the API for OncoTree and serves the frontend assets.

From the root of the Go source:

    ```
    cd oncotree/web/src/main/go
    swag init -g ./cmd/server/main.go -o ./docs
    ```

This will generate API documentation in the docs folder.

### Running the Backend

To start the backend server:

    ```
    go run ./cmd/server
    ```

## OncoTree Mapping Tool

The OncoTree Mapping Tool was developed to facilitate the mapping of OncoTree codes between different OncoTree release versions. To learn more about the OncoTree Converter tool, please refer to the [OncoTree Mapping documentation](/docs/OncoTree-Mapping-Tool.md).

## OncoTree History Modeling
The OncoTree is an evolving ontology and as such has gone through several iterations since its conception to improve the standardization of cancer type diagnoses from a clinical perspective. As such, some nodes on the OncoTree possess a complex revision history whereas others have a much more simple revision history.

## OncoTree API

For APIs, please see the [OncoTree Swagger page](http://oncotree.mskcc.org/#/home?tab=api)


## License
<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
