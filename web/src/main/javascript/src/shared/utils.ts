import { OncoTreeNode, OncoTreeSearchOption } from "@oncokb/oncotree";
import { SEARCH_BY_FIELD_INFO, SearchByField } from "./constants";

export function traverseBreadthFirst(
  root: OncoTreeNode,
  action: (node: OncoTreeNode) => void,
) {
  let nodes = [root];
  while (nodes.length > 0) {
    const newNodes: OncoTreeNode[] = [];
    for (const node of nodes) {
      action(node);
      for (const child of Object.values(node.children)) {
        newNodes.push(child);
      }
    }
    nodes = newNodes;
  }
}

export function getSearchOptions(
  root: OncoTreeNode,
  field: SearchByField,
): OncoTreeSearchOption[] {
  const options: { label: string; value: string }[] = [];

  if (field === SearchByField.NAME) {
    traverseBreadthFirst(root, (node) => {
      pushSearchOptionIfUnique(options, {
        label: getNodeLabel(node),
        value: node.code,
      });
    });
  } else {
    traverseBreadthFirst(root, (node) => {
      const fieldValue = node[SEARCH_BY_FIELD_INFO[field].dataName];
      if (fieldValue) {
        pushSearchOptionIfUnique(options, {
          label: fieldValue.toString(),
          value: fieldValue.toString(),
        });
      }
    });
  }

  options.sort((a, b) =>
    a.label.toLowerCase().localeCompare(b.label.toLowerCase()),
  );
  return options;
}

export function getNodeLabel(node: OncoTreeNode) {
  return `${node.name} (${node.code})`;
}

function pushSearchOptionIfUnique(
  searchOptions: OncoTreeSearchOption[],
  option: OncoTreeSearchOption,
) {
  if (
    !searchOptions.some((searchOption) => searchOption.value === option.value)
  ) {
    searchOptions.push(option);
  }
}

export type OncoTreeStats = { numCancerTypes: number; numTissues: number };

export function getTreeStats(root: OncoTreeNode): OncoTreeStats {
  const stats: OncoTreeStats = { numCancerTypes: 0, numTissues: 0 };

  traverseBreadthFirst(root, (node) => {
    if (node.level > 0) {
      stats.numCancerTypes++;
    }
    if (node.level === 1) {
      stats.numTissues++;
    }
  });

  return stats;
}

export function isValidField(field: string | null): field is SearchByField {
  return Object.values(SearchByField).includes(field as SearchByField);
}

export function getTotalWidth(element: HTMLElement) {
  const styles = window.getComputedStyle(element);
  return (
    element.offsetWidth +
    parseFloat(styles.marginLeft) +
    parseFloat(styles.marginRight)
  );
}
