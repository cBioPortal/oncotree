import { useEffect, useRef } from "react";
import OncoTree, { OncoTreeNode, ToolbarAction } from "@oncokb/oncotree";
import ToolbarItem from "../../components/Toolbar/ToolbarItem";

const TREE_CONTAINER_ID = "oncotree-container";

export interface IHomeProps {
  oncoTreeData: OncoTreeNode;
  oncoTree: OncoTree | undefined;
  onOncoTreeInit: (oncoTree: OncoTree) => void;
}

export default function Home({
  oncoTreeData,
  oncoTree,
  onOncoTreeInit,
}: IHomeProps) {
  const treeContainerRef = useRef<HTMLDivElement>(null);
  const dataRef = useRef<typeof oncoTreeData | undefined>();

  useEffect(() => {
    const versionChanged = dataRef.current !== oncoTreeData;

    if (treeContainerRef.current && versionChanged) {
      if (treeContainerRef.current.children.length > 0) {
        treeContainerRef.current.innerHTML = "";
      }

      const oncoTree = new OncoTree(TREE_CONTAINER_ID, oncoTreeData);
      onOncoTreeInit(oncoTree);
    }

    dataRef.current = oncoTreeData;
  }, [oncoTreeData, onOncoTreeInit]);

  return (
    <>
      <div style={{ position: "relative", top: 8, left: 8 }}>
        {oncoTree && (
          <div style={{ position: "absolute" }}>
            <ToolbarItem oncoTree={oncoTree} type={ToolbarAction.EXPAND} />
            <div style={{ marginTop: 8 }} />
            <ToolbarItem oncoTree={oncoTree} type={ToolbarAction.COLLAPSE} />
          </div>
        )}
      </div>
      <div
        ref={treeContainerRef}
        id={TREE_CONTAINER_ID}
        style={{
          height: "100%",
          width: "100%",
          display: "flex",
          alignItems: "center",
        }}
      />
    </>
  );
}
