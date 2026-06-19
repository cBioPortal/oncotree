import { Route, Routes, useLocation, useSearchParams } from "react-router-dom";
import Home from "./pages/Home/Home";
import Header from "./components/Header/Header";
import "./app.scss";
import OncoTree, { OncoTreeNode } from "@oncokb/oncotree";
import { useCallback, useEffect, useState } from "react";
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
const READY_MESSAGE_TYPE = "oncotree-ready";

function App() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const version = searchParams.get("version") ?? DEFAULT_VERSION;

  const [oncoTreeData, setOncoTreeData] = useState<OncoTreeNode>();
  const [oncoTree, setOncoTree] = useState<OncoTree>();
  const [annotations, setAnnotations] = useState<AnnotationMap | null>(null);

  async function fetchData(apiIdentifier: string) {
    const response = await fetch(
      `${ONCOTREE_TREE_URL}/?&version=${apiIdentifier}`,
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
      if (!data || data.type !== ANNOTATIONS_MESSAGE_TYPE) {
        return;
      }
      const payload = data.annotations;
      if (payload === null || payload === undefined) {
        setAnnotations(null);
        return;
      }
      if (typeof payload === "string") {
        const decoded = decodeAnnotations(payload);
        if (decoded) {
          setAnnotations(decoded);
        }
        return;
      }
      if (typeof payload === "object" && !Array.isArray(payload)) {
        setAnnotations(normalizeAnnotationMap(payload as Record<string, unknown>));
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
      <Header
        oncoTree={oncoTree}
        oncoTreeData={oncoTreeData}
        onVersionChange={(version) => {
          fetchData(version.api_identifier);
        }}
      />
      <div className="app-content-container">
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
              />
            }
          />
          <Route path={PageRoutes.NEWS} element={<News />} />
          <Route path={PageRoutes.MAPPING} element={<Mapping />} />
          <Route path={PageRoutes.ABOUT} element={<About />} />
        </Routes>
      </div>
      <Footer />
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
