import OncoTree, {
  buildExpandItem,
  buildCollapseItem,
  ToolbarAction,
} from "@oncokb/oncotree";
import { useEffect, useRef } from "react";

const EXPAND_ITEM_CONTAINER_ID = "oncotree-expand-item-container";
const COLLAPSE_ITEM_CONTAINER_ID = "oncotree-collapse-item-container";

export interface IToolbarProps {
  oncoTree: OncoTree;
  type: ToolbarAction.EXPAND | ToolbarAction.COLLAPSE;
}

export default function ToolbarItem({ oncoTree, type }: IToolbarProps) {
  const toolbarContainerRef = useRef<HTMLDivElement>(null);
  const toolbarItemId =
    type === ToolbarAction.EXPAND
      ? EXPAND_ITEM_CONTAINER_ID
      : COLLAPSE_ITEM_CONTAINER_ID;

  useEffect(() => {
    if (toolbarContainerRef.current) {
      toolbarContainerRef.current.innerHTML = "";
      type === ToolbarAction.EXPAND
        ? buildExpandItem(toolbarItemId, oncoTree)
        : buildCollapseItem(toolbarItemId, oncoTree);
    }
  }, [oncoTree, toolbarItemId, type]);

  return <div ref={toolbarContainerRef} id={toolbarItemId} />;
}
