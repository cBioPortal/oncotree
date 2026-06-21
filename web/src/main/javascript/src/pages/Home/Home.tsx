import { useEffect, useRef, useState } from "react";
import OncoTree, { OncoTreeNode } from "@oncokb/oncotree";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faExpand, faCompress } from "@fortawesome/free-solid-svg-icons";
import AnnotationPanel from "../../components/AnnotationPanel/AnnotationPanel";
import AnnotationOverlay from "../../components/AnnotationOverlay/annotationOverlay";
import { AnnotationMap } from "../../shared/annotations";
import styles from "./home.module.scss";

const TREE_CONTAINER_ID = "oncotree-container";

/** A parent node forking into two children — "expand". */
function ExpandTreeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="1em" height="1em" aria-hidden="true">
      <line x1="6" y1="12" x2="17" y2="6" stroke="currentColor" strokeWidth="1.6" />
      <line x1="6" y1="12" x2="17" y2="18" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="5" cy="12" r="3.2" fill="currentColor" />
      <circle cx="18.5" cy="6" r="2.6" fill="currentColor" />
      <circle cx="18.5" cy="18" r="2.6" fill="currentColor" />
    </svg>
  );
}

/** A single node with its subtree tucked away — "collapse". */
function CollapseTreeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="1em" height="1em" aria-hidden="true">
      <line x1="6" y1="12" x2="16" y2="12" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="5" cy="12" r="3.2" fill="currentColor" />
      <circle cx="18.5" cy="12" r="2.6" fill="currentColor" />
    </svg>
  );
}

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
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    const onChange = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener("fullscreenchange", onChange);
    return () => document.removeEventListener("fullscreenchange", onChange);
  }, []);

  function toggleFullscreen() {
    // Fullscreen the content area (tree + toolbar + panel), not just the SVG.
    const target = treeContainerRef.current?.parentElement;
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else {
      target?.requestFullscreen?.();
    }
  }

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
      <div className={styles.toolbar}>
        <button
          className={styles.toolButton}
          title={isFullscreen ? "Exit full screen" : "Full screen"}
          aria-label={isFullscreen ? "Exit full screen" : "Full screen"}
          onClick={toggleFullscreen}
        >
          <FontAwesomeIcon icon={isFullscreen ? faCompress : faExpand} />
        </button>
        {oncoTree && (
          <>
            <button
              className={styles.toolButton}
              title="Expand all"
              aria-label="Expand all"
              onClick={() => oncoTree.expand()}
            >
              <ExpandTreeIcon />
            </button>
            <button
              className={styles.toolButton}
              title="Collapse all"
              aria-label="Collapse all"
              onClick={() => oncoTree.collapse()}
            >
              <CollapseTreeIcon />
            </button>
          </>
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
