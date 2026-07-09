import { useEffect, useRef, useState } from "react";
import OncoTree, { OncoTreeNode } from "@oncokb/oncotree";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faExpand, faCompress } from "@fortawesome/free-solid-svg-icons";
import { toast } from "react-toastify";
import AnnotationPanel from "../../components/AnnotationPanel/AnnotationPanel";
import AnnotationOverlay from "../../components/AnnotationOverlay/annotationOverlay";
import { AnnotationMap } from "../../shared/annotations";
import styles from "./home.module.scss";

const TREE_CONTAINER_ID = "oncotree-container";

type TreeDataNode = {
  code?: string;
  name?: string;
  children?: Record<string, TreeDataNode>;
};

/** All annotated codes in a node's subtree (its own + descendants). */
function annotatedSubtreeCodes(
  node: TreeDataNode | undefined,
  annotations: AnnotationMap | null,
): string[] {
  if (!node || !annotations) {
    return [];
  }
  const result: string[] = [];
  const seen = new Set<string>();
  const stack: TreeDataNode[] = [node];
  while (stack.length > 0) {
    const current = stack.pop() as TreeDataNode;
    const up = current.code?.toUpperCase();
    if (up && !seen.has(up) && annotations[up] !== undefined) {
      seen.add(up);
      result.push(up);
    }
    for (const child of Object.values(current.children ?? {})) {
      stack.push(child);
    }
  }
  return result;
}

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
  /** Codes the host has selected, rendered with a checkmark on the tree. */
  selectedCodes?: string[];
  /** Hide the annotation input panel (host drives annotations via postMessage). */
  hideAnnotationPanel?: boolean;
}

export default function Home({
  oncoTreeData,
  oncoTree,
  onOncoTreeInit,
  annotations,
  onAnnotationsChange,
  selectedCodes,
  hideAnnotationPanel = false,
}: IHomeProps) {
  const treeContainerRef = useRef<HTMLDivElement>(null);
  const dataRef = useRef<typeof oncoTreeData | undefined>();
  const overlayRef = useRef<AnnotationOverlay | null>(null);
  const annotationsRef = useRef(annotations);
  annotationsRef.current = annotations;
  const selectedCodesRef = useRef(selectedCodes);
  selectedCodesRef.current = selectedCodes;
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
      // Rejects when embedded in an iframe without allow="fullscreen"; the host
      // must opt in, so just surface a hint instead of throwing.
      target?.requestFullscreen?.().catch(() => {
        toast.info(
          'Full screen is blocked. If embedded, add allow="fullscreen" to the iframe.',
        );
      });
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
    overlay.setSelectedCodes(selectedCodesRef.current ?? []);
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

  useEffect(() => {
    overlayRef.current?.setSelectedCodes(selectedCodes ?? []);
  }, [selectedCodes]);

  // Click model: the badge selects (handled in the overlay); the node name/dot
  // expand/collapse via the library. Additionally, expanding a collapsed parent
  // selects every annotated cancer type in its subtree. Runs in the capture
  // phase so the node's collapsed state is read before the library toggles it.
  useEffect(() => {
    const container = treeContainerRef.current;
    if (!container || !oncoTree) {
      return;
    }
    const onClick = (event: MouseEvent) => {
      if (window.parent === window) {
        return;
      }
      const target = event.target as Element;
      // Badge clicks are select-only and handled by the overlay.
      if (target?.closest?.(".annotation-overlay")) {
        return;
      }
      const node = target?.closest?.("g.node") as SVGGElement | null;
      if (!node) {
        return;
      }
      const datum = (
        node as unknown as {
          __data__?: { _children?: unknown; data?: TreeDataNode };
        }
      ).__data__;
      // `_children` set => currently collapsed => this click expands it.
      if (!datum?._children) {
        return;
      }
      const codes = annotatedSubtreeCodes(datum.data, annotationsRef.current);
      if (codes.length > 0) {
        window.parent.postMessage(
          { type: "oncotree-node-click", codes, mode: "add" },
          "*",
        );
      }
    };
    container.addEventListener("click", onClick, true);
    return () => container.removeEventListener("click", onClick, true);
  }, [oncoTree]);

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
