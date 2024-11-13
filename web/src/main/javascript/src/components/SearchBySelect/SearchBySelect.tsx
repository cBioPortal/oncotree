import { useEffect, useRef, useState } from "react";
import { faChevronDown } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  DEFAULT_FIELD,
  SEARCH_BY_FIELD_INFO,
  SearchByField,
} from "../../shared/constants";
import style from "./search-by-select.module.scss";
import searchBarStyle from "./../SearchBar/search-bar.module.scss";
import { useSearchParams } from "react-router-dom";
import { isValidField } from "../../shared/utils";

const HORIZONTAL_MARGIN = 20;
export interface ISearchBySelectProps {
  mobileView?: boolean;
}

export default function SearchBySelect({
  mobileView = false,
}: ISearchBySelectProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const field = searchParams.get("field");

  const [showingFieldOptions, setShowingFieldOptions] = useState(false);

  const searchByContainerRef = useRef<HTMLDivElement>(null);
  const searchByOptionsContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        searchByContainerRef.current &&
        searchByOptionsContainerRef.current &&
        !searchByContainerRef.current.contains(event.target as HTMLElement) &&
        !searchByOptionsContainerRef.current.contains(
          event.target as HTMLElement,
        ) &&
        showingFieldOptions
      ) {
        setShowingFieldOptions(false);
      }
    }

    document.addEventListener("click", handleClickOutside);

    return () => {
      document.removeEventListener("click", handleClickOutside);
    };
  }, [showingFieldOptions]);

  function getSearchByOptionsContent() {
    return Object.values(SearchByField).map((option) => {
      return (
        <div
          className={searchBarStyle["search-bar-option"]}
          key={option}
          onClick={() => {
            searchParams.set("field", option);
            setSearchParams(searchParams);
            setShowingFieldOptions(false);
          }}
        >
          <span>{SEARCH_BY_FIELD_INFO[option].displayName}</span>
        </div>
      );
    });
  }

  return (
    <>
      <div
        className={style["search-by-container"]}
        ref={searchByContainerRef}
        style={{
          marginRight: HORIZONTAL_MARGIN,
          marginLeft: mobileView ? 0 : HORIZONTAL_MARGIN,
        }}
        onClick={() => {
          setShowingFieldOptions((prev) => !prev);
        }}
        onMouseDown={(event) => {
          if (event.detail > 1) {
            event.preventDefault();
          }
        }}
      >
        <span>
          {!mobileView && "Search by "}
          <b style={{ color: mobileView ? "black" : "lightblue" }}>
            {isValidField(field)
              ? SEARCH_BY_FIELD_INFO[field].displayName
              : SEARCH_BY_FIELD_INFO[DEFAULT_FIELD].displayName}
          </b>
        </span>
        <FontAwesomeIcon
          icon={faChevronDown}
          style={{
            marginLeft: 8,
            transition: "transform .2s ease-in-out",
            transform: showingFieldOptions ? "rotate(180deg)" : "rotate(0deg)",
          }}
        />
      </div>
      <div
        className={style["search-by-options-container"]}
        ref={searchByOptionsContainerRef}
        style={{
          display: showingFieldOptions ? "block" : "none",
          marginLeft: mobileView ? 0 : HORIZONTAL_MARGIN - 10,
        }}
      >
        {getSearchByOptionsContent()}
      </div>
    </>
  );
}
