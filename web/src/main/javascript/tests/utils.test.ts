import { OncoTreeNode, OncoTreeSearchOption } from "@oncokb/oncotree";
import {
  OncoTreeStats,
  traverseBreadthFirst,
  getSearchOptions,
  getTreeStats,
} from "../src/shared/utils";
import { SearchByField } from "../src/shared/constants";

describe("utils tests", () => {
  const data = {
    name: "Pancreas",
    code: "PANCREAS",
    level: 0,
    children: {
      BREAST: {
        name: "Breast",
        code: "BREAST",
        level: 1,
        children: {},
      },
      BONE: {
        name: "Bone",
        code: "BONE",
        level: 1,
        children: {
          EYE: {
            name: "Eye",
            code: "EYE",
            level: 2,
            children: {
              SKIN: {
                name: "Skin",
                code: "SKIN",
                level: 3,
                children: {},
              },
            },
          },
          LUNG: {
            name: "Lung",
            code: "LUNG",
            level: 2,
            children: {},
          },
        },
      },
      LIVER: {
        name: "Liver",
        code: "LIVER",
        level: 1,
        children: {
          "KIDNEY:": {
            name: "Kidney",
            code: "KIDNEY",
            level: 2,
            children: {},
          },
        },
      },
    },
  } as unknown as OncoTreeNode;

  it("breadthFirstSearch should visit every node and perform an action", () => {
    const nodeNames: string[] = [];
    traverseBreadthFirst(data, (node) => {
      nodeNames.push(node.name);
    });

    expect(nodeNames).toEqual([
      "Pancreas",
      "Breast",
      "Bone",
      "Liver",
      "Eye",
      "Lung",
      "Kidney",
      "Skin",
    ]);
  });

  it("getSearchOptions should create search options (sorted by name) from oncotree JSON data", () => {
    expect(getSearchOptions(data, SearchByField.NAME)).toEqual<
      OncoTreeSearchOption[]
    >([
      { label: "Bone (BONE)", value: "BONE" },
      { label: "Breast (BREAST)", value: "BREAST" },
      { label: "Eye (EYE)", value: "EYE" },
      { label: "Kidney (KIDNEY)", value: "KIDNEY" },
      { label: "Liver (LIVER)", value: "LIVER" },
      { label: "Lung (LUNG)", value: "LUNG" },
      { label: "Pancreas (PANCREAS)", value: "PANCREAS" },
      { label: "Skin (SKIN)", value: "SKIN" },
    ]);
  });

  it("getTreeStats should return number of cancer types and tissues", () => {
    expect(getTreeStats(data)).toEqual<OncoTreeStats>({
      numCancerTypes: 7,
      numTissues: 3,
    });
  });
});
