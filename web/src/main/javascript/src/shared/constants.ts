import { OncoTreeNode } from "@oncokb/oncotree";

export const ONCOTREE_BASE_URL = window.location.origin
export const ONCOTREE_TREE_URL = `${ONCOTREE_BASE_URL}/api/tumorTypes/tree`;
export const ONCOTREE_VERSIONS_URL = `${ONCOTREE_BASE_URL}/api/versions`;
export const ONCOTREE_SWAGGER_URL = `${ONCOTREE_BASE_URL}/swagger/index.html`;
export const COMMUNITY_GROUP_URL = "https://groups.google.com/g/oncotree-users";

export const DEFAULT_VERSION = "oncotree_latest_stable";
export const DEVELOPMENT_VERSION = "oncotree_development";

export const TOAST_SUCCESS_ID = "toast-success";

export enum PageRoutes {
  HOME = "/",
  NEWS = "/news",
  MAPPING = "/mapping",
  ABOUT = "/about",
}

export enum SearchByField {
  NAME = "NAME",
  CODE = "CODE",
  MAIN_TYPE = "MAIN_TYPE",
}

export const SEARCH_BY_FIELD_INFO: {
  [key in SearchByField]: {
    dataName: keyof OncoTreeNode;
    displayName: string;
    searchBarPlaceHolder: string;
  };
} = {
  [SearchByField.NAME]: {
    dataName: "name",
    displayName: "Name",
    searchBarPlaceHolder: "Enter cancer type",
  },
  [SearchByField.CODE]: {
    dataName: "code",
    displayName: "Code",
    searchBarPlaceHolder: "Enter code",
  },
  [SearchByField.MAIN_TYPE]: {
    dataName: "mainType",
    displayName: "Main Type",
    searchBarPlaceHolder: "Enter main type",
  },
};

export const DEFAULT_FIELD = SearchByField.NAME;
