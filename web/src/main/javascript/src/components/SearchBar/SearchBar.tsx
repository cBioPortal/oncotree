import OncoTree, {
  D3OncoTreeNode,
  OncoTreeNode,
  OncoTreeSearchOption,
} from "@oncokb/oncotree";
import { useCallback, useEffect, useMemo, useState } from "react";
import ReactSelect, {
  ClearIndicatorProps,
  ControlProps,
  GroupBase,
  components,
} from "react-select";
import {
  getNodeLabel,
  getSearchOptions,
  isValidField,
} from "../../shared/utils";
import { faArrowUp, faArrowDown } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { css } from "@emotion/css";
import variables from "../../variables.module.scss";
import { useSearchParams } from "react-router-dom";
import {
  DEFAULT_FIELD,
  SEARCH_BY_FIELD_INFO,
  SearchByField,
} from "../../shared/constants";

export interface ISearchBarProps {
  oncoTreeData: OncoTreeNode;
  oncoTree: OncoTree | undefined;
  mobileView?: boolean;
}

const NEXT_BUTTON_DATA_TYPE = "nextButton";
const PREV_BUTTON_DATA_TYPE = "prevButton";
const USER_INPUT_VALUE = "USER_INPUT";

export default function SearchBar({
  oncoTreeData,
  oncoTree,
  mobileView = false,
}: ISearchBarProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const search = searchParams.get("search");
  const field = searchParams.get("field");

  const [input, setInput] = useState("");
  const [cancerTypeResults, setCancerTypeResults] =
    useState<D3OncoTreeNode[]>();
  const [cancerTypeResultsIndex, setCancerTypeResultsIndex] =
    useState<number>();

  const resetSearch = useCallback(() => {
    oncoTree?.collapse();
    setInput("");
    searchParams.delete("search");
    setSearchParams(searchParams);
    setCancerTypeResults(undefined);
    setCancerTypeResultsIndex(undefined);
  }, [
    oncoTree,
    setInput,
    searchParams,
    setSearchParams,
    setCancerTypeResults,
    setCancerTypeResultsIndex,
  ]);

  useEffect(() => {
    if (!isValidField(field)) {
      searchParams.set("field", DEFAULT_FIELD);
      setSearchParams(searchParams);
    }
  }, [field, searchParams, setSearchParams]);

  const defaultOptions = useMemo(() => {
    if (!isValidField(field)) {
      return [];
    }
    return getSearchOptions(oncoTreeData, field);
  }, [oncoTreeData, field]);

  const options = useMemo(() => {
    let matchedCancerTypeIndex = -1;
    let matchedCodeIndex = -1;
    for (let i = 0; i < defaultOptions.length; i++) {
      const formattedInput = input.trim().toLowerCase();
      if (defaultOptions[i].label.toLowerCase() === formattedInput) {
        matchedCancerTypeIndex = i;
        break;
      } else if (
        field === SearchByField.NAME &&
        defaultOptions[i].value.trim().toLowerCase() === formattedInput
      ) {
        matchedCodeIndex = i;
        break;
      }
    }

    if (input.length === 0) {
      return defaultOptions;
    }
    if (matchedCancerTypeIndex > -1) {
      return [
        defaultOptions[matchedCancerTypeIndex],
        ...defaultOptions.slice(0, matchedCancerTypeIndex),
        ...defaultOptions.slice(matchedCancerTypeIndex + 1),
      ];
    }
    if (matchedCodeIndex > -1) {
      return [
        { label: input, value: USER_INPUT_VALUE },
        defaultOptions[matchedCodeIndex],
        ...defaultOptions.slice(0, matchedCodeIndex),
        ...defaultOptions.slice(matchedCodeIndex + 1),
      ];
    }
    return [{ label: input, value: USER_INPUT_VALUE }, ...defaultOptions];
  }, [defaultOptions, input, field]);

  const handleSearch = useCallback(
    (keyword: string) => {
      if (!oncoTree || !isValidField(field)) {
        return;
      }

      const results = oncoTree.search((node) => {
        let searchString: string;
        if (field === SearchByField.NAME) {
          searchString = getNodeLabel(node.data);
        } else {
          searchString = node.data[SEARCH_BY_FIELD_INFO[field].dataName];
        }

        if (!searchString) {
          return false;
        }

        return searchString.toLowerCase().includes(keyword.toLowerCase());
      });
      setCancerTypeResults(results);
      setCancerTypeResultsIndex(0);
      if (results.length > 0) {
        oncoTree.focus(results[0]);
      }
    },
    [oncoTree, field],
  );

  useEffect(() => {
    if (search && oncoTree) {
      handleSearch(search);
    }
  }, [search, oncoTree, handleSearch]);

  function getSearchBarValue(): OncoTreeSearchOption | null {
    if (!search) {
      return null;
    }

    const option = options.find((option) => option.label === search);
    if (option) {
      return option;
    }
    return { label: search, value: USER_INPUT_VALUE };
  }

  const Control = useCallback(
    (
      props: ControlProps<
        OncoTreeSearchOption,
        false,
        GroupBase<OncoTreeSearchOption>
      >,
    ) => {
      const onMouseDown = props.innerProps.onMouseDown;
      props.innerProps.onMouseDown = (event) => {
        if (
          !(event.target instanceof HTMLDivElement) ||
          event.target.dataset.type === NEXT_BUTTON_DATA_TYPE ||
          event.target.dataset.type === PREV_BUTTON_DATA_TYPE
        ) {
          return undefined;
        }

        const dataAttributes = event.target.dataset;
        if (
          dataAttributes.type === NEXT_BUTTON_DATA_TYPE ||
          dataAttributes.type === PREV_BUTTON_DATA_TYPE
        ) {
          // make sure not on arrows
          return undefined;
        }
        return onMouseDown?.(event);
      };

      return <components.Control {...props} />;
    },
    [],
  );

  return (
    <div style={{ width: mobileView ? "unset" : 400 }}>
      <ReactSelect
        inputValue={input}
        value={getSearchBarValue()}
        placeholder={
          isValidField(field)
            ? SEARCH_BY_FIELD_INFO[field].searchBarPlaceHolder
            : SEARCH_BY_FIELD_INFO[DEFAULT_FIELD].searchBarPlaceHolder
        }
        isClearable
        backspaceRemovesValue
        onInputChange={(newValue) => {
          setInput(newValue);
        }}
        onChange={(selection) => {
          if (selection) {
            handleSearch(selection.label);
            searchParams.set("search", selection.label);
            setSearchParams(searchParams);
          } else {
            resetSearch();
          }
        }}
        options={options}
        components={{
          ClearIndicator,
          Control,
        }}
        styles={{
          option(base, props) {
            if (props.isSelected) {
              return { ...base, backgroundColor: variables.primary };
            } else {
              return { ...base, color: "black" };
            }
          },
          control(base) {
            return {
              ...base,
              minHeight: 42,
              minWidth: mobileView ? "unset" : 250,
            };
          },
        }}
        theme={(theme) => {
          return {
            ...theme,
            colors: {
              ...theme.colors,
              primary: variables.primary,
              neutral90: variables.white,
            },
          };
        }}
      />
    </div>
  );

  function ClearIndicator(
    props: ClearIndicatorProps<
      OncoTreeSearchOption,
      false,
      GroupBase<OncoTreeSearchOption>
    >,
  ) {
    const inputStyle = props.getStyles("input", { ...props, isHidden: false });
    const inputColor = inputStyle.color ?? "black";
    const clearIndicatorClass = css(props.getStyles("clearIndicator", props));

    const resultsAndIndexDefined =
      cancerTypeResults !== undefined && cancerTypeResultsIndex !== undefined;

    function getResultsNumberSpan() {
      if (!resultsAndIndexDefined) {
        return undefined;
      }

      if (cancerTypeResults.length === 0) {
        return (
          <span
            className={clearIndicatorClass}
            style={{
              color: Array.isArray(inputColor) ? inputColor[0] : inputColor,
            }}
          >
            0/0
          </span>
        );
      }

      return (
        <span
          className={clearIndicatorClass}
          style={{
            color: Array.isArray(inputColor) ? inputColor[0] : inputColor,
          }}
        >{`${cancerTypeResultsIndex + 1}/${cancerTypeResults.length}`}</span>
      );
    }

    const getPreviousResult = useCallback(() => {
      if (!resultsAndIndexDefined) {
        return;
      }

      let newIndex = cancerTypeResults.length - 1;
        if (cancerTypeResultsIndex !== 0) {
          newIndex = cancerTypeResultsIndex - 1;
        }
        oncoTree?.focus(cancerTypeResults[newIndex]);
        setCancerTypeResultsIndex(newIndex);
    }, [resultsAndIndexDefined, cancerTypeResults, cancerTypeResultsIndex]);

    const getNextResult = useCallback(() => {
      if (!resultsAndIndexDefined) {
        return;
      }

      let newIndex = 0;
        if (
          cancerTypeResultsIndex !==
          cancerTypeResults.length - 1
        ) {
          newIndex = cancerTypeResultsIndex + 1;
        }
        oncoTree?.focus(cancerTypeResults[newIndex]);
        setCancerTypeResultsIndex(newIndex);
    }, [resultsAndIndexDefined, cancerTypeResults, cancerTypeResultsIndex]);

    return (
      <>
        {getResultsNumberSpan()}
        {resultsAndIndexDefined && cancerTypeResults.length > 0 && (
          <div style={{ userSelect: "none", display: "flex" }}>
            <div
              data-type={PREV_BUTTON_DATA_TYPE}
              className={clearIndicatorClass}
              onClick={mobileView ? undefined : getPreviousResult}
              onTouchStart={mobileView ? getPreviousResult : undefined}
            >
              <FontAwesomeIcon
                style={{ pointerEvents: "none" }}
                icon={faArrowUp}
              />
            </div>
            <div
              data-type={NEXT_BUTTON_DATA_TYPE}
              className={clearIndicatorClass}
              onClick={mobileView ? undefined : getNextResult}
              onTouchStart={mobileView ? getNextResult : undefined}
            >
              <FontAwesomeIcon
                style={{ pointerEvents: "none" }}
                icon={faArrowDown}
              />
            </div>
          </div>
        )}
        <components.ClearIndicator {...props} />
      </>
    );
  }
}
