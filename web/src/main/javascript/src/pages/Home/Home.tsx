import { useEffect, useRef } from "react";
import OncoTree, { OncoTreeNode, ToolbarAction } from "@oncokb/oncotree";
import ToolbarItem from "../../components/Toolbar/ToolbarItem";
import AnnotationPanel from "../../components/AnnotationPanel/AnnotationPanel";
import AnnotationOverlay from "../../components/AnnotationOverlay/annotationOverlay";
import { AnnotationMap } from "../../shared/annotations";

const TREE_CONTAINER_ID = "oncotree-container";

export interface IHomeProps {
  oncoTreeData: OncoTreeNode;
  oncoTree: OncoTree | undefined;
  onOncoTreeInit: (oncoTree: OncoTree) => void;
  annotations: AnnotationMap | null;
  onAnnotationsChange: (annotations: AnnotationMap | null) => void;
  /** Hide the annotation input panel (host drives annotations via postMessage). */
  hideAnnotationPanel?: boolean;
}

export default function Home({
  oncoTreeData,
  oncoTree,
  onOncoTreeInit,
  annotations,
  onAnnotationsChange,
  hideAnnotationPanel = false,
}: IHomeProps) {
  const treeContainerRef = useRef<HTMLDivElement>(null);
  const dataRef = useRef<typeof oncoTreeData | undefined>();
  const overlayRef = useRef<AnnotationOverlay | null>(null);
  const annotationsRef = useRef(annotations);
  annotationsRef.current = annotations;

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

  useEffect(() => {
    if (!oncoTree || !treeContainerRef.current) {
      return;
    }
    const overlay = new AnnotationOverlay(treeContainerRef.current);
    overlayRef.current = overlay;
    overlay.setAnnotations(annotationsRef.current);
    // The tree renders with transitions; re-apply once nodes have settled.
    const raf = requestAnimationFrame(() =>
      overlay.setAnnotations(annotationsRef.current),
    );

    return () => {
      cancelAnimationFrame(raf);
      overlay.destroy();
      overlayRef.current = null;
    };
  }, [oncoTree]);

  useEffect(() => {
    overlayRef.current?.setAnnotations(annotations);
  }, [annotations]);

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
      {!hideAnnotationPanel && (
        <AnnotationPanel
          oncoTreeData={oncoTreeData}
          annotations={annotations}
          onApply={onAnnotationsChange}
          onClear={() => onAnnotationsChange(null)}
        />
      )}
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
