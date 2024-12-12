import { Route, Routes, useLocation, useSearchParams } from "react-router-dom";
import Home from "./pages/Home/Home";
import Header from "./components/Header/Header";
import "./app.scss";
import OncoTree, { OncoTreeNode } from "@oncokb/oncotree";
import { useCallback, useEffect, useState } from "react";
import "react-toastify/dist/ReactToastify.css";
import { Bounce, ToastContainer } from "react-toastify";
import News from "./pages/News/News";
import {
  ONCOTREE_TREE_URL,
  PageRoutes,
} from "./shared/constants";
import Mapping from "./pages/Mapping/Mapping";
import Footer from "./components/Footer/footer";
import About from "./pages/About/About";

function App() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

  const [oncoTreeData, setOncoTreeData] = useState<OncoTreeNode>();
  const [oncoTree, setOncoTree] = useState<OncoTree>();

  async function fetchData(apiIdentifier: string) {
    const response = await fetch(
      `${ONCOTREE_TREE_URL}/?&version=${apiIdentifier}`,
    );
    const data: { [name: string]: OncoTreeNode } = await response.json();
    const formattedData = Object.values(data)[0];
    setOncoTreeData(formattedData);
  }

  useEffect(() => {
    const version = searchParams.get('version');
    if (version) {
      fetchData(version);
    }
  }, [searchParams]);

  useEffect(() => {
    if (location.pathname !== PageRoutes.HOME) {
      setSearchParams(undefined);
    }
  }, [location.pathname, setSearchParams]);

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
