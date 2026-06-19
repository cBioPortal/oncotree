import { OncoTreeNode } from "@oncokb/oncotree";
import { traverseBreadthFirst } from "./utils";

/**
 * A single annotation attached to an OncoTree code. All fields are optional so
 * that callers can supply a number, a label, a gene list, or any combination.
 */
export type AnnotationValue = {
  value?: number;
  label?: string;
  genes?: string[];
};

/** Annotations keyed by (upper-cased) OncoTree code. */
export type AnnotationMap = Record<string, AnnotationValue>;

export type ParseResult = {
  annotations: AnnotationMap;
  /** Fatal problems that prevented parsing (input rejected). */
  errors: string[];
  /** Codes that parsed fine but are not present in the current tree. */
  unknownCodes: string[];
  /** Number of codes successfully mapped to a value. */
  count: number;
};

const NUMERIC_FIELDS = ["value", "count", "n", "samples"];
const LABEL_FIELDS = ["label", "name", "text"];
const GENE_FIELDS = ["genes", "gene", "geneList"];

function asNumber(raw: unknown): number | undefined {
  if (typeof raw === "number" && !isNaN(raw)) {
    return raw;
  }
  if (typeof raw === "string" && raw.trim() !== "" && !isNaN(Number(raw))) {
    return Number(raw);
  }
  return undefined;
}

function asGeneList(raw: unknown): string[] | undefined {
  if (Array.isArray(raw)) {
    const genes = raw.map((g) => String(g).trim()).filter(Boolean);
    return genes.length > 0 ? genes : undefined;
  }
  return undefined;
}

/** Coerce an arbitrary JSON value for a single code into an AnnotationValue. */
function normalizeValue(raw: unknown): AnnotationValue | null {
  if (raw === null || raw === undefined) {
    return null;
  }

  const num = asNumber(raw);
  if (num !== undefined) {
    return { value: num };
  }

  if (typeof raw === "string") {
    return { label: raw };
  }

  const genes = asGeneList(raw);
  if (genes) {
    return { genes };
  }

  if (typeof raw === "object") {
    const record = raw as Record<string, unknown>;
    const annotation: AnnotationValue = {};

    for (const field of NUMERIC_FIELDS) {
      const candidate = asNumber(record[field]);
      if (candidate !== undefined) {
        annotation.value = candidate;
        break;
      }
    }
    for (const field of LABEL_FIELDS) {
      if (typeof record[field] === "string") {
        annotation.label = record[field] as string;
        break;
      }
    }
    for (const field of GENE_FIELDS) {
      const candidate = asGeneList(record[field]);
      if (candidate) {
        annotation.genes = candidate;
        break;
      }
    }

    if (
      annotation.value === undefined &&
      annotation.label === undefined &&
      annotation.genes === undefined
    ) {
      annotation.label = JSON.stringify(raw);
    }
    return annotation;
  }

  return null;
}

/** Upper-case codes and normalize each value into an AnnotationValue. */
export function normalizeAnnotationMap(
  raw: Record<string, unknown>,
): AnnotationMap {
  const map: AnnotationMap = {};
  for (const [rawCode, rawValue] of Object.entries(raw)) {
    const code = rawCode.trim().toUpperCase();
    if (!code) {
      continue;
    }
    const value = normalizeValue(rawValue);
    if (value) {
      map[code] = value;
    }
  }
  return map;
}

function detectDelimiter(line: string): string {
  if (line.includes("\t")) {
    return "\t";
  }
  return ",";
}

const HEADER_TOKENS = new Set(
  [...NUMERIC_FIELDS, ...LABEL_FIELDS, ...GENE_FIELDS, "code", "oncotree"].map(
    (token) => token.toLowerCase(),
  ),
);

function looksLikeHeader(cells: string[]): boolean {
  return cells.slice(1).every((cell) => HEADER_TOKENS.has(cell.toLowerCase()));
}

/**
 * Parse a delimited table where the first column is an OncoTree code. A single
 * remaining column becomes a numeric value (or a label if non-numeric); multiple
 * remaining columns become a gene list.
 */
function parseDelimited(text: string): Record<string, unknown> {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  if (lines.length === 0) {
    return {};
  }

  const delimiter = detectDelimiter(lines[0]);
  const result: Record<string, unknown> = {};

  const firstCells = lines[0].split(delimiter).map((cell) => cell.trim());
  const startIndex = looksLikeHeader(firstCells) ? 1 : 0;

  for (let i = startIndex; i < lines.length; i++) {
    const cells = lines[i].split(delimiter).map((cell) => cell.trim());
    const code = cells[0];
    if (!code) {
      continue;
    }
    const rest = cells.slice(1).filter((cell) => cell !== "");
    if (rest.length === 0) {
      continue;
    }
    result[code] = rest.length === 1 ? rest[0] : rest;
  }

  return result;
}

function buildValidCodeSet(root: OncoTreeNode | undefined): Set<string> {
  const codes = new Set<string>();
  if (root) {
    traverseBreadthFirst(root, (node) => {
      if (node.code) {
        codes.add(node.code.toUpperCase());
      }
    });
  }
  return codes;
}

/**
 * Parse pasted/uploaded annotation text. Accepts a JSON object (code -> value)
 * or a CSV/TSV table. Codes are matched against the current tree (case
 * insensitive); unknown codes are still kept but surfaced in `unknownCodes`.
 */
export function parseAnnotations(
  input: string,
  root: OncoTreeNode | undefined,
): ParseResult {
  const result: ParseResult = {
    annotations: {},
    errors: [],
    unknownCodes: [],
    count: 0,
  };

  const trimmed = input.trim();
  if (!trimmed) {
    result.errors.push("No annotation data provided.");
    return result;
  }

  let raw: Record<string, unknown>;
  try {
    const parsed = JSON.parse(trimmed);
    if (Array.isArray(parsed) || typeof parsed !== "object" || parsed === null) {
      throw new Error("not an object");
    }
    raw = parsed as Record<string, unknown>;
  } catch {
    raw = parseDelimited(trimmed);
    if (Object.keys(raw).length === 0) {
      result.errors.push(
        "Could not parse input as JSON or CSV. Expected a JSON object mapping OncoTree codes to values, or a CSV with a code in the first column.",
      );
      return result;
    }
  }

  const validCodes = buildValidCodeSet(root);

  result.annotations = normalizeAnnotationMap(raw);
  for (const code of Object.keys(result.annotations)) {
    result.count++;
    if (validCodes.size > 0 && !validCodes.has(code)) {
      result.unknownCodes.push(code);
    }
  }

  if (result.count === 0) {
    result.errors.push("No valid annotations found in the input.");
  }

  return result;
}

export type ColorScale = {
  hasNumeric: boolean;
  min: number;
  max: number;
  colorFor: (value: number) => string;
};

const SCALE_LOW: [number, number, number] = [222, 235, 247]; // #deebf7
const SCALE_HIGH: [number, number, number] = [8, 81, 156]; // #08519c

function mix(
  low: [number, number, number],
  high: [number, number, number],
  t: number,
): string {
  const channel = (index: number) =>
    Math.round(low[index] + (high[index] - low[index]) * t);
  return `rgb(${channel(0)}, ${channel(1)}, ${channel(2)})`;
}

/** Build a sequential color scale over the numeric values in the map. */
export function buildColorScale(annotations: AnnotationMap): ColorScale {
  const values = Object.values(annotations)
    .map((annotation) => annotation.value)
    .filter((value): value is number => typeof value === "number");

  if (values.length === 0) {
    return {
      hasNumeric: false,
      min: 0,
      max: 0,
      colorFor: () => mix(SCALE_LOW, SCALE_HIGH, 1),
    };
  }

  const min = Math.min(...values);
  const max = Math.max(...values);

  return {
    hasNumeric: true,
    min,
    max,
    colorFor: (value: number) => {
      const raw = max === min ? 1 : (value - min) / (max - min);
      // Aggregated (summed) values can exceed the per-node max; clamp to scale.
      const t = Math.max(0, Math.min(1, raw));
      return mix(SCALE_LOW, SCALE_HIGH, t);
    },
  };
}

export function formatNumber(value: number): string {
  if (Number.isInteger(value)) {
    return value.toLocaleString();
  }
  return value.toLocaleString(undefined, { maximumFractionDigits: 2 });
}

const URL_PARAM = "annotations";

/** Encode annotations for use in the `?annotations=` URL parameter. */
export function encodeAnnotations(annotations: AnnotationMap): string {
  const json = JSON.stringify(annotations);
  return btoa(unescape(encodeURIComponent(json)));
}

function parseAnnotationObject(text: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(text);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      return null;
    }
    return parsed as Record<string, unknown>;
  } catch {
    return null;
  }
}

/**
 * Decode the `?annotations=` URL parameter into an AnnotationMap. Accepts either
 * raw (URL-encoded) JSON — e.g. `?annotations={"LUAD":1204}` — or a
 * base64/base64url-encoded JSON object produced by {@link encodeAnnotations}.
 */
export function decodeAnnotations(encoded: string): AnnotationMap | null {
  const trimmed = encoded.trim();

  let raw = parseAnnotationObject(trimmed);

  if (!raw) {
    try {
      const base64 = trimmed.replace(/-/g, "+").replace(/_/g, "/");
      raw = parseAnnotationObject(decodeURIComponent(escape(atob(base64))));
    } catch {
      raw = null;
    }
  }

  return raw ? normalizeAnnotationMap(raw) : null;
}

export const ANNOTATIONS_URL_PARAM = URL_PARAM;

export const SAMPLE_ANNOTATIONS = JSON.stringify(
  {
    LUAD: { label: "1,204 samples", value: 1204 },
    LUSC: { label: "489 samples", value: 489 },
    BRCA: { label: "967 samples", value: 967 },
    PAAD: { label: "186 samples", value: 186 },
    COAD: { label: "542 samples", value: 542 },
    GB: { genes: ["EGFR", "PTEN", "TP53", "NF1"] },
    SKCM: { genes: ["BRAF", "NRAS", "NF1"] },
    PRAD: { value: 333 },
  },
  null,
  2,
);
