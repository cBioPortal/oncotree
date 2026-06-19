import { useMemo, useRef, useState } from "react";
import { OncoTreeNode } from "@oncokb/oncotree";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faTags,
  faUpload,
  faXmark,
  faLink,
  faTrash,
} from "@fortawesome/free-solid-svg-icons";
import { toast } from "react-toastify";
import {
  AnnotationMap,
  ANNOTATIONS_URL_PARAM,
  buildColorScale,
  encodeAnnotations,
  formatNumber,
  parseAnnotations,
  SAMPLE_ANNOTATIONS,
} from "../../shared/annotations";
import styles from "./annotation-panel.module.scss";

export interface IAnnotationPanelProps {
  oncoTreeData: OncoTreeNode;
  annotations: AnnotationMap | null;
  onApply: (annotations: AnnotationMap) => void;
  onClear: () => void;
}

type Feedback =
  | { type: "error"; text: string }
  | { type: "warning"; text: string }
  | { type: "success"; text: string }
  | null;

export default function AnnotationPanel({
  oncoTreeData,
  annotations,
  onApply,
  onClear,
}: IAnnotationPanelProps) {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const [feedback, setFeedback] = useState<Feedback>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const annotationCount = annotations ? Object.keys(annotations).length : 0;
  const colorScale = useMemo(
    () => buildColorScale(annotations ?? {}),
    [annotations],
  );

  function handleApply() {
    const result = parseAnnotations(text, oncoTreeData);
    if (result.errors.length > 0) {
      setFeedback({ type: "error", text: result.errors.join(" ") });
      return;
    }

    onApply(result.annotations);

    if (result.unknownCodes.length > 0) {
      const preview = result.unknownCodes.slice(0, 5).join(", ");
      const more =
        result.unknownCodes.length > 5
          ? ` (+${result.unknownCodes.length - 5} more)`
          : "";
      setFeedback({
        type: "warning",
        text: `Loaded ${result.count} annotation${
          result.count === 1 ? "" : "s"
        }. ${result.unknownCodes.length} code${
          result.unknownCodes.length === 1 ? " is" : "s are"
        } not in this OncoTree version: ${preview}${more}.`,
      });
    } else {
      setFeedback({
        type: "success",
        text: `Loaded ${result.count} annotation${
          result.count === 1 ? "" : "s"
        }.`,
      });
    }
  }

  function handleSample() {
    setText(SAMPLE_ANNOTATIONS);
    setFeedback(null);
  }

  function handleFile(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setText(typeof reader.result === "string" ? reader.result : "");
      setFeedback(null);
    };
    reader.onerror = () =>
      setFeedback({ type: "error", text: "Could not read the selected file." });
    reader.readAsText(file);
    event.target.value = "";
  }

  function handleClear() {
    onClear();
    setText("");
    setFeedback(null);
  }

  async function handleShare() {
    if (!annotations || annotationCount === 0) {
      return;
    }
    const url = new URL(window.location.href);
    url.searchParams.set(
      ANNOTATIONS_URL_PARAM,
      encodeAnnotations(annotations),
    );
    try {
      await navigator.clipboard.writeText(url.toString());
      toast.success("Shareable link copied to clipboard");
    } catch {
      setFeedback({
        type: "warning",
        text: "Could not copy automatically. Link: " + url.toString(),
      });
    }
  }

  return (
    <div className={styles.container}>
      <button className={styles.toggle} onClick={() => setOpen((v) => !v)}>
        <FontAwesomeIcon icon={open ? faXmark : faTags} />
        Annotations
        {annotationCount > 0 && (
          <span className={styles.count}>{annotationCount}</span>
        )}
      </button>

      {open && (
        <div className={styles.panel}>
          <h3 className={styles.heading}>Annotation Overlay</h3>
          <p className={styles.hint}>
            Paste a JSON object mapping OncoTree codes to values, or a CSV with
            the code in the first column. Example:{" "}
            <code>{`{"LUAD": 1204, "GBM": {"genes": ["EGFR"]}}`}</code>
          </p>

          <textarea
            className={styles.textarea}
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={'{\n  "LUAD": {"label": "1204 samples", "value": 1204}\n}'}
            spellCheck={false}
          />

          <div className={styles.actions}>
            <button
              className={`${styles.button} ${styles.primary}`}
              onClick={handleApply}
            >
              Apply
            </button>
            <button
              className={styles.button}
              onClick={() => fileInputRef.current?.click()}
            >
              <FontAwesomeIcon icon={faUpload} />
              Upload
            </button>
            {annotationCount > 0 && (
              <button className={styles.button} onClick={handleClear}>
                <FontAwesomeIcon icon={faTrash} />
                Clear
              </button>
            )}
            <input
              ref={fileInputRef}
              type="file"
              accept=".json,.csv,.tsv,.txt,application/json,text/csv"
              className={styles.hiddenInput}
              onChange={handleFile}
            />
          </div>

          <div className={styles.secondaryActions}>
            <button className={styles.link} onClick={handleSample}>
              Load sample data
            </button>
            {annotationCount > 0 && (
              <button className={styles.link} onClick={handleShare}>
                <FontAwesomeIcon icon={faLink} /> Copy share link
              </button>
            )}
          </div>

          {feedback && (
            <div className={`${styles.message} ${styles[feedback.type]}`}>
              {feedback.text}
            </div>
          )}

          {annotationCount > 0 && (
            <div className={styles.legend}>
              <div className={styles.legendTitle}>Legend</div>
              <div className={styles.legendRow}>
                <span className={styles.ringSwatch} />
                <span>Annotated (node keeps its OncoTree color)</span>
              </div>
              {colorScale.hasNumeric && (
                <>
                  <div className={styles.gradient} />
                  <div className={styles.gradientLabels}>
                    <span>{formatNumber(colorScale.min)}</span>
                    <span>{formatNumber(colorScale.max)}</span>
                  </div>
                  <div className={styles.legendCaption}>value badge color</div>
                </>
              )}
              <div className={styles.legendRow}>
                <span className={styles.swatch} />
                <span>Gene list / text label badge</span>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
