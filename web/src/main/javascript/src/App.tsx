import { Route, Routes, useLocation, useSearchParams } from "react-router-dom";
import Home from "./pages/Home/Home";
import Header from "./components/Header/Header";
import "./app.scss";
import OncoTree, { D3OncoTreeNode, OncoTreeNode } from "@oncokb/oncotree";
import { useCallback, useEffect, useRef, useState } from "react";
import "react-toastify/dist/ReactToastify.css";
import { Bounce, toast, ToastContainer } from "react-toastify";
import News from "./pages/News/News";
import {
  DEFAULT_VERSION,
  ONCOTREE_TREE_URL,
  PageRoutes,
} from "./shared/constants";
import Mapping from "./pages/Mapping/Mapping";
import Footer from "./components/Footer/footer";
import About from "./pages/About/About";
import {
  AnnotationMap,
  ANNOTATIONS_URL_PARAM,
  decodeAnnotations,
  normalizeAnnotationMap,
} from "./shared/annotations";

const ANNOTATIONS_MESSAGE_TYPE = "oncotree-annotations";
const SEARCH_MESSAGE_TYPE = "oncotree-search";
const READY_MESSAGE_TYPE = "oncotree-ready";
const SEARCH_RESULT_MESSAGE_TYPE = "oncotree-search-result";

/** Match a tree node by code, name, or its annotation (gene/label/value). */
function nodeMatchesQuery(
  node: D3OncoTreeNode,
  lower: string,
  raw: string,
  annotations: AnnotationMap | null,
): boolean {
  const code = node.data.code?.toLowerCase() ?? "";
  const name = node.data.name?.toLowerCase() ?? "";
  if (code.includes(lower) || name.includes(lower)) {
    return true;
  }
  const annotation = annotations?.[node.data.code?.toUpperCase() ?? ""];
  if (annotation) {
    if (annotation.label?.toLowerCase().includes(lower)) {
      return true;
    }
    if (annotation.genes?.some((gene) => gene.toLowerCase().includes(lower))) {
      return true;
    }
    if (annotation.value !== undefined && String(annotation.value) === raw) {
      return true;
    }
  }
  return false;
}

function App() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const version = searchParams.get("version") ?? DEFAULT_VERSION;
  // `?embed` (or ?embed=1) hides the site chrome (header/footer) for embedding;
  // the host then drives the view via postMessage.
  const embedParam = searchParams.get("embed");
  const embed =
    embedParam !== null && embedParam !== "0" && embedParam !== "false";

  const [oncoTreeData, setOncoTreeData] = useState<OncoTreeNode>();
  const [oncoTree, setOncoTree] = useState<OncoTree>();
  const [annotations, setAnnotations] = useState<AnnotationMap | null>(null);

  // Refs so the (mount-once) postMessage listener always sees current values.
  const oncoTreeRef = useRef(oncoTree);
  oncoTreeRef.current = oncoTree;
  const annotationsRef = useRef(annotations);
  annotationsRef.current = annotations;

  async function fetchData(apiIdentifier: string) {
    const response = await fetch(
      `${ONCOTREE_TREE_URL}?version=${apiIdentifier}`,
    );
    const data: { [name: string]: OncoTreeNode } = await response.json();
    const formattedData = Object.values(data)[0];
    setOncoTreeData(formattedData);
  }

  useEffect(() => {
    try {
      fetchData(version);
    } catch {
      toast.error("Error fetching OncoTree data");
    }
  }, [version]);

  useEffect(() => {
    if (location.pathname !== PageRoutes.HOME) {
      setSearchParams(undefined);
    }
  }, [location.pathname, setSearchParams]);

  useEffect(() => {
    const encoded = searchParams.get(ANNOTATIONS_URL_PARAM);
    if (encoded) {
      const decoded = decodeAnnotations(encoded);
      if (decoded) {
        setAnnotations(decoded);
      } else {
        toast.error("Could not read annotations from the URL");
      }
    }
    // Load shared annotations once on initial mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Embed API: an embedding page can push annotations via
  // `iframe.contentWindow.postMessage({ type: "oncotree-annotations",
  // annotations: <object | JSON string | null> }, "*")`. `null`/`{}` clears.
  useEffect(() => {
    function onMessage(event: MessageEvent) {
      const data = event.data;
      if (!data) {
        return;
      }

      if (data.type === ANNOTATIONS_MESSAGE_TYPE) {
        const payload = data.annotations;
        if (payload === null || payload === undefined) {
          setAnnotations(null);
        } else if (typeof payload === "string") {
          const decoded = decodeAnnotations(payload);
          if (decoded) {
            setAnnotations(decoded);
          }
        } else if (typeof payload === "object" && !Array.isArray(payload)) {
          setAnnotations(
            normalizeAnnotationMap(payload as Record<string, unknown>),
          );
        }
        return;
      }

      if (data.type === SEARCH_MESSAGE_TYPE) {
        const tree = oncoTreeRef.current;
        if (!tree) {
          return;
        }
        const query = typeof data.query === "string" ? data.query.trim() : "";
        if (!query || data.clear) {
          // Reset highlight and collapse back to the top level.
          tree.search(() => false);
          return;
        }
        const lower = query.toLowerCase();
        const results = tree.search((node) =>
          nodeMatchesQuery(node, lower, query, annotationsRef.current),
        );
        if (results.length > 0) {
          tree.focus(results[0]);
        }
        if (window.parent !== window) {
          window.parent.postMessage(
            { type: SEARCH_RESULT_MESSAGE_TYPE, query, count: results.length },
            "*",
          );
        }
        return;
      }
    }

    window.addEventListener("message", onMessage);
    // Let an embedding parent know the tree is ready to receive annotations.
    if (window.parent !== window) {
      window.parent.postMessage({ type: READY_MESSAGE_TYPE }, "*");
    }
    return () => window.removeEventListener("message", onMessage);
  }, []);

  const onOncoTreeInit = useCallback((oncoTree: OncoTree) => {
    setOncoTree(oncoTree);
  }, []);

  if (!oncoTreeData) {
    return <>Loading...</>;
  }

  return (
    <div className="app-container">
      {!embed && (
        <Header
          oncoTree={oncoTree}
          oncoTreeData={oncoTreeData}
          onVersionChange={(version) => {
            fetchData(version.api_identifier);
          }}
        />
      )}
      <div
        className={
          embed
            ? "app-content-container app-content-container--embed"
            : "app-content-container"
        }
      >
        <Routes>
          <Route
            path={PageRoutes.HOME}
            element={
              <Home
                oncoTreeData={oncoTreeData}
                oncoTree={oncoTree}
                onOncoTreeInit={onOncoTreeInit}
                annotations={annotations}
                onAnnotationsChange={setAnnotations}
                hideAnnotationPanel={embed}
              />
            }
          />
          <Route path={PageRoutes.NEWS} element={<News />} />
          <Route path={PageRoutes.MAPPING} element={<Mapping />} />
          <Route path={PageRoutes.ABOUT} element={<About />} />
        </Routes>
      </div>
      {!embed && <Footer />}
      <ToastContainer
        position="top-right"
        autoClose={1800}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="light"
        transition={Bounce}
      />
    </div>
  );
}

export default App;
