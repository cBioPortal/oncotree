import { useEffect, useState } from "react";
import {
  DEFAULT_VERSION,
  DEVELOPMENT_VERSION,
  ONCOTREE_VERSIONS_URL,
} from "../../shared/constants";
import { toast } from "react-toastify";
import ReactSelect from "react-select";
import { OncoTreeSearchOption } from "@oncokb/oncotree";
import { Tooltip } from "react-tooltip";
import variables from "../../variables.module.scss";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faInfoCircle, faWarning } from "@fortawesome/free-solid-svg-icons";
import { OncoTreeStats } from "../../shared/utils";
import { useSearchParams } from "react-router-dom";

export type Version = {
  api_identifier: string;
  description: string;
  release_date: string;
  visible: boolean;
};

type VersionOption = OncoTreeSearchOption & {
  data: Version;
};

export interface IVersionSelectProps {
  stats: OncoTreeStats;
  onVersionChange: (version: Version) => void;
  mobileView?: boolean;
}

export default function VersionSelect({
  stats,
  onVersionChange,
  mobileView = false,
}: IVersionSelectProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const version = searchParams.get("version");

  const [versionOptions, setVersionOptions] = useState<VersionOption[]>();

  useEffect(() => {
    async function getVersions() {
      try {
        const response = await fetch(ONCOTREE_VERSIONS_URL);
        const data: Version[] = await response.json();

        const versionOptions: VersionOption[] = [];
        for (const version of data) {
          const versionOption: VersionOption = {
            label: version.api_identifier,
            value: version.api_identifier,
            data: version,
          };
          versionOptions.push(versionOption);
        }

        setVersionOptions(versionOptions);
      } catch {
        toast.error("Error fetching versions");
      }
    }

    getVersions();
  }, []);

  function getSelectedVersion() {
    if (!version) {
      return undefined;
    }

    const versionOption = versionOptions?.find(
      (option) => option.data.api_identifier === version,
    );
    if (!versionOption) {
      return undefined;
    }
    return versionOption;
  }

  useEffect(() => {
    if (
      !version ||
      (versionOptions &&
        !versionOptions.find((option) => option.value === version))
    ) {
      searchParams.set("version", DEFAULT_VERSION);
      setSearchParams(searchParams);
    }
  }, [version, versionOptions, searchParams, setSearchParams]);

  function getTooltipContent() {
    return `
            Includes <b>${stats.numCancerTypes}</b> cancer type${stats.numCancerTypes === 1 ? "" : "s"} from <b>${stats.numTissues}</b> tissue${stats.numTissues === 1 ? "" : "s"}.
            <br /><br />
            ${getSelectedVersion()?.data.description}
        `;
  }

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center" }}>
        <ReactSelect
          placeholder={version || DEFAULT_VERSION}
          value={getSelectedVersion()}
          options={versionOptions?.sort((a, b) => {
            if (a.data.visible && !b.data.visible) {
              return -1;
            }
            if (!a.data.visible && b.data.visible) {
              return 1;
            }
            return (new Date(a.data.release_date).getTime() - new Date(b.data.release_date).getTime()) * -1;
          })}
          onChange={(newValue) => {
            if (newValue) {
              searchParams.set("version", newValue.value);
              searchParams.delete("search");
              setSearchParams(searchParams);
              onVersionChange(newValue.data);
            }
          }}
          styles={{
            container(base) {
              if (mobileView) {
                return { ...base, flexGrow: 1 };
              }
              return base;
            },
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
                minWidth: mobileView ? "100%" : 265,
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
        <div
          style={{ marginLeft: 8 }}
          data-tooltip-id="version-tooltip"
          data-tooltip-html={getTooltipContent()}
        >
          <FontAwesomeIcon icon={faInfoCircle} />
        </div>
        <Tooltip
          id="version-tooltip"
          place="bottom"
          style={{ maxWidth: 200, zIndex: 1 }}
        />
      </div>
      {version === DEVELOPMENT_VERSION && (
        <span
          style={{
            position: "absolute",
            width: "100%",
            color: mobileView ? "#dc3545" : "#f99",
            marginTop: -0.5,
            fontSize: ".87rem",
            textOverflow: "clip",
          }}
        >
          <FontAwesomeIcon icon={faWarning} style={{ marginRight: 6 }} />
          <span>
            Subject to change <b>without notice</b>
          </span>
        </span>
      )}
    </div>
  );
}
