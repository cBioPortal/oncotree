import { defineConfig } from "vitepress";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "OncoTree",
  description:
    "OncoTree, an open-source cancer classification system, to address fast-evolving needs in clinical reporting of genomic sequencing results and associated oncology research.",
  srcDir: "../docs",
  outDir: "../public",
  themeConfig: {
    siteTitle: false,
    aside: "left",
  },
});
