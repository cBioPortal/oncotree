import style from "./header.module.scss";
import mainLogo from "../../assets/oncotree_logo_no_bg.svg";
import { Link, useLocation, useNavigate } from "react-router-dom";
import SearchBar from "../SearchBar/SearchBar";
import OncoTree, { OncoTreeNode } from "@oncokb/oncotree";
import { PageRoutes } from "../../shared/constants";
import VersionSelect, { Version } from "../VersionSelect/VersionSelect";
import { useEffect, useMemo, useRef, useState } from "react";
import { OncoTreeStats, getTotalWidth, getTreeStats } from "../../shared/utils";
import SearchBySelect from "../SearchBySelect/SearchBySelect";
import IconButton from "../icon-buttons/IconButton";
import { faNavicon, faSearch } from "@fortawesome/free-solid-svg-icons";

const LINKS_SECTION_HORIZONTAL_MARGIN = 30;
const MOBILE_MENU_HORIZONTAL_MARGIN = 32;
export interface IHeaderProps {
  oncoTreeData: OncoTreeNode;
  oncoTree: OncoTree | undefined;
  onVersionChange: (version: Version) => void;
}

export default function Header({
  oncoTreeData,
  oncoTree,
  onVersionChange,
}: IHeaderProps) {
  const location = useLocation();
  const navigate = useNavigate();

  const [searchSectionHidden, setSearchSectionHidden] = useState(true);
  const [headerLinksHidden, setHeaderLinksHidden] = useState(true);

  const [showMobileSearch, setShowMobileSearch] = useState(false);
  const [showMobileLinks, setShowMobileLinks] = useState(false);

  const headerRef = useRef<HTMLImageElement>(null);
  const oncoTreeLogoRef = useRef<HTMLImageElement>(null);
  const searchSectionRef = useRef<HTMLDivElement>(null);
  const headerLinksRef = useRef<HTMLDivElement>(null);
  const mobileSearchRef = useRef<HTMLDivElement>(null);
  const mobileLinksRef = useRef<HTMLDivElement>(null);
  const mobileSearchBarRef = useRef<HTMLDivElement>(null);
  const mobileVersionSelectRef = useRef<HTMLDivElement>(null);

  const treeStats: OncoTreeStats = useMemo(() => {
    return getTreeStats(oncoTreeData);
  }, [oncoTreeData]);

  useEffect(() => {
    function checkOverflow() {
      if (
        !headerRef.current ||
        !oncoTreeLogoRef.current ||
        !headerLinksRef.current
      ) {
        return;
      }

      const headerWidth = getTotalWidth(headerRef.current);
      const oncoTreeLogoWidth = getTotalWidth(oncoTreeLogoRef.current);
      const searchSectionWidth = searchSectionRef.current
        ? getTotalWidth(searchSectionRef.current)
        : 0;
      const headerLinksWidth = getTotalWidth(headerLinksRef.current);

      setHeaderLinksHidden(
        headerWidth < oncoTreeLogoWidth + searchSectionWidth + headerLinksWidth,
      );

      if (searchSectionRef.current) {
        setSearchSectionHidden(
          headerWidth < oncoTreeLogoWidth + searchSectionWidth + 120,
        );
      }
    }

    checkOverflow();
    window.addEventListener("resize", checkOverflow);

    return () => {
      window.removeEventListener("resize", checkOverflow);
    };
  }, [location, setHeaderLinksHidden, setSearchSectionHidden]);

  useEffect(() => {
    if (!headerLinksHidden) {
      setShowMobileLinks(false);
    }
  }, [headerLinksHidden]);

  useEffect(() => {
    if (!searchSectionHidden) setShowMobileSearch(false);
  }, [searchSectionHidden]);

  return (
    <>
      <div ref={headerRef} className={style.header}>
        <img
          ref={oncoTreeLogoRef}
          src={mainLogo}
          style={{ height: "110%", cursor: "pointer" }}
          onClick={() => {
            navigate(PageRoutes.HOME);
          }}
        />
        {location.pathname === PageRoutes.HOME && (
          <div
            ref={searchSectionRef}
            style={{
              display: "flex",
              flexShrink: 0,
              alignItems: "center",
              visibility: searchSectionHidden ? "hidden" : "unset",
            }}
          >
            <SearchBySelect />
            <SearchBar oncoTreeData={oncoTreeData} oncoTree={oncoTree} />
            <div style={{ marginRight: 20 }} />
            <VersionSelect
              onVersionChange={onVersionChange}
              stats={treeStats}
            />
          </div>
        )}
        <div style={{ flexGrow: 1 }} />
        <div
          ref={headerLinksRef}
          style={{
            marginLeft: LINKS_SECTION_HORIZONTAL_MARGIN,
            marginRight: LINKS_SECTION_HORIZONTAL_MARGIN,
            visibility: headerLinksHidden ? "hidden" : "unset",
          }}
        >
          <Link
            className={style["header-link"]}
            style={{ marginRight: 40 }}
            to={PageRoutes.HOME}
          >
            Home
          </Link>
          <Link
            className={style["header-link"]}
            style={{ marginRight: 40 }}
            to={PageRoutes.NEWS}
          >
            News
          </Link>
          <Link
            className={style["header-link"]}
            style={{ marginRight: 40 }}
            to={PageRoutes.MAPPING}
          >
            Mapping
          </Link>
          <Link className={style["header-link"]} to={PageRoutes.ABOUT}>
            About
          </Link>
        </div>
        {headerLinksHidden && (
          <div
            style={{
              position: "absolute",
              right: LINKS_SECTION_HORIZONTAL_MARGIN,
            }}
          >
            {location.pathname === PageRoutes.HOME && searchSectionHidden && (
              <IconButton
                icon={faSearch}
                style={{ marginRight: 20 }}
                onClick={() => {
                  setShowMobileSearch((show) => !show);
                  setShowMobileLinks(false);
                }}
              />
            )}
            <IconButton
              icon={faNavicon}
              onClick={() => {
                setShowMobileLinks((show) => !show);
                setShowMobileSearch(false);
              }}
            />
          </div>
        )}
      </div>
      <div
        className={`${style["mobile-container"]} ${style["mobile-links"]}`}
        style={
          showMobileLinks
            ? { transform: `translateY(-1px)` }
            : {
                transform: `translateY(${0 - (mobileLinksRef.current?.scrollHeight ?? Number.MAX_SAFE_INTEGER)}px)`,
                visibility: "hidden",
              }
        }
        ref={mobileLinksRef}
      >
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            marginLeft: MOBILE_MENU_HORIZONTAL_MARGIN,
          }}
        >
          <Link
            className={style["header-link"]}
            style={{ marginBottom: 10 }}
            to={PageRoutes.HOME}
            onClick={() => {
              setShowMobileLinks(false);
              setShowMobileSearch(false);
            }}
          >
            Home
          </Link>
          <Link
            className={style["header-link"]}
            style={{ marginBottom: 10 }}
            to={PageRoutes.NEWS}
            onClick={() => {
              setShowMobileLinks(false);
              setShowMobileSearch(false);
            }}
          >
            News
          </Link>
          <Link
            className={style["header-link"]}
            style={{ marginBottom: 10 }}
            to={PageRoutes.MAPPING}
            onClick={() => {
              setShowMobileLinks(false);
              setShowMobileSearch(false);
            }}
          >
            Mapping
          </Link>
          <Link
            className={style["header-link"]}
            to={PageRoutes.ABOUT}
            onClick={() => {
              setShowMobileLinks(false);
              setShowMobileSearch(false);
            }}
          >
            About
          </Link>
        </div>
      </div>
      <div
        className={`${style["mobile-container"]} ${style["mobile-search"]}`}
        style={
          showMobileSearch
            ? { transform: `translateY(-1px)`, width: "100%" }
            : {
                transform: `translateY(${0 - (mobileSearchRef.current?.scrollHeight ?? Number.MAX_SAFE_INTEGER)}px)`,
                visibility: "hidden",
              }
        }
        ref={mobileSearchRef}
      >
        <div
          style={{
            color: "black",
            width: "100%",
            display: "flex",
            justifyContent: "center",
            paddingRight: 20,
            paddingLeft: 20,
          }}
        >
          <div>
            <div
              style={{
                height: mobileSearchBarRef.current?.clientHeight,
                display: "flex",
                alignItems: "center",
                marginBottom: 6,
              }}
            >
              <SearchBySelect mobileView />
            </div>
            <div
              style={{
                height: mobileVersionSelectRef.current?.clientHeight,
                display: "flex",
                alignItems: "center",
              }}
            >
              <b style={{ fontSize: ".9rem" }}>Version</b>
            </div>
          </div>
          <div style={{ marginRight: 8 }} />
          <div style={{ flexGrow: 1, maxWidth: 400 }}>
            <div ref={mobileSearchBarRef} style={{ marginBottom: 6 }}>
              <SearchBar
                oncoTreeData={oncoTreeData}
                oncoTree={oncoTree}
                mobileView
              />
            </div>
            <div ref={mobileVersionSelectRef}>
              <VersionSelect
                onVersionChange={onVersionChange}
                stats={treeStats}
                mobileView
              />
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
