import {
  AnnotationMap,
  AnnotationValue,
  buildColorScale,
  ColorScale,
  formatNumber,
} from "../../shared/annotations";

const SVG_NS = "http://www.w3.org/2000/svg";
const OVERLAY_CLASS = "annotation-overlay";
const HALO_CLASS = "annotation-halo";
// Neutral marker so an annotated node keeps its own OncoTree color; the ring
// only signals "this node is annotated".
const ANNOTATED_RING = "#f59f00";
// Badge color for non-numeric (gene list / text label) annotations.
const CATEGORICAL_BADGE = "#495057";
const REAPPLY_DEBOUNCE_MS = 60;

type D3Datum = {
  data?: { code?: string; name?: string };
};

function getDatum(element: Element): D3Datum | undefined {
  return (element as unknown as { __data__?: D3Datum }).__data__;
}

function createSvgElement<K extends keyof SVGElementTagNameMap>(
  tag: K,
): SVGElementTagNameMap[K] {
  return document.createElementNS(SVG_NS, tag);
}

function badgeText(annotation: AnnotationValue): string | null {
  if (annotation.value !== undefined) {
    return formatNumber(annotation.value);
  }
  if (annotation.genes && annotation.genes.length > 0) {
    return `${annotation.genes.length} gene${annotation.genes.length === 1 ? "" : "s"}`;
  }
  if (annotation.label) {
    return annotation.label.length > 16
      ? `${annotation.label.slice(0, 15)}…`
      : annotation.label;
  }
  return null;
}

function tooltipLines(
  datum: D3Datum,
  annotation: AnnotationValue,
): { title: string; rows: string[] } {
  const name = datum.data?.name ?? "";
  const code = datum.data?.code ?? "";
  const title = name ? `${name} (${code})` : code;
  const rows: string[] = [];
  if (annotation.value !== undefined) {
    rows.push(`Value: ${formatNumber(annotation.value)}`);
  }
  if (annotation.label && annotation.label !== badgeText(annotation)) {
    rows.push(annotation.label);
  } else if (annotation.label && annotation.value === undefined) {
    rows.push(annotation.label);
  }
  if (annotation.genes && annotation.genes.length > 0) {
    rows.push(`Genes: ${annotation.genes.join(", ")}`);
  }
  return { title, rows };
}

/**
 * Decorates the OncoTree SVG with annotation overlays. The OncoTree package
 * re-renders nodes (with d3 transitions) on every expand/collapse and re-applies
 * its own circle styling, so overlays are appended as extra children of each
 * `g.node` and re-applied whenever the SVG mutates.
 */
export default class AnnotationOverlay {
  private container: HTMLElement;
  private annotations: AnnotationMap = {};
  private colorScale: ColorScale = buildColorScale({});
  private observer: MutationObserver;
  private reapplyTimer: number | undefined;
  private tooltip: HTMLDivElement | undefined;

  constructor(container: HTMLElement) {
    this.container = container;
    this.observer = new MutationObserver(() => this.scheduleReapply());
  }

  setAnnotations(annotations: AnnotationMap | null): void {
    this.annotations = annotations ?? {};
    this.colorScale = buildColorScale(this.annotations);
    this.apply();
    if (Object.keys(this.annotations).length > 0) {
      this.startObserving();
    } else {
      this.observer.disconnect();
    }
  }

  destroy(): void {
    this.observer.disconnect();
    if (this.reapplyTimer !== undefined) {
      window.clearTimeout(this.reapplyTimer);
    }
    this.clearOverlays();
    this.tooltip?.remove();
    this.tooltip = undefined;
  }

  private getSvgGroup(): SVGGElement | null {
    return this.container.querySelector("svg g");
  }

  private startObserving(): void {
    const group = this.getSvgGroup();
    if (group) {
      this.observer.observe(group, { childList: true, subtree: true });
    }
  }

  private scheduleReapply(): void {
    if (this.reapplyTimer !== undefined) {
      window.clearTimeout(this.reapplyTimer);
    }
    this.reapplyTimer = window.setTimeout(() => {
      this.apply();
    }, REAPPLY_DEBOUNCE_MS);
  }

  private clearOverlays(): void {
    this.container
      .querySelectorAll(`.${OVERLAY_CLASS}, .${HALO_CLASS}`)
      .forEach((element) => element.remove());
  }

  private apply(): void {
    // Avoid reacting to our own DOM insertions.
    this.observer.disconnect();
    this.clearOverlays();

    const hasAnnotations = Object.keys(this.annotations).length > 0;
    if (hasAnnotations) {
      const nodes = this.container.querySelectorAll<SVGGElement>("g.node");
      nodes.forEach((node) => this.decorateNode(node));
    }

    if (hasAnnotations) {
      this.startObserving();
    }
  }

  private decorateNode(node: SVGGElement): void {
    const datum = getDatum(node);
    const code = datum?.data?.code?.toUpperCase();
    if (!datum || !code) {
      return;
    }
    const annotation = this.annotations[code];
    if (!annotation) {
      return;
    }

    // The badge carries the data color (value magnitude); the halo stays a
    // neutral marker so the node's own OncoTree color remains readable.
    const badgeColor =
      annotation.value !== undefined && this.colorScale.hasNumeric
        ? this.colorScale.colorFor(annotation.value)
        : CATEGORICAL_BADGE;

    const halo = createSvgElement("circle");
    halo.setAttribute("class", HALO_CLASS);
    halo.setAttribute("r", "9");
    halo.setAttribute("fill", ANNOTATED_RING);
    halo.setAttribute("fill-opacity", "0.12");
    halo.setAttribute("stroke", ANNOTATED_RING);
    halo.setAttribute("stroke-width", "2.5");
    halo.style.pointerEvents = "all";
    halo.style.cursor = "pointer";
    node.insertBefore(halo, node.firstChild);

    const label = badgeText(annotation);
    const overlay = createSvgElement("g");
    overlay.setAttribute("class", OVERLAY_CLASS);
    overlay.style.pointerEvents = "all";
    overlay.style.cursor = "pointer";

    if (label) {
      const paddingX = 5;
      const charWidth = 6.2;
      const width = Math.max(16, label.length * charWidth + paddingX * 2);
      const height = 15;
      const y = -23;

      const rect = createSvgElement("rect");
      rect.setAttribute("x", `${-width / 2}`);
      rect.setAttribute("y", `${y}`);
      rect.setAttribute("width", `${width}`);
      rect.setAttribute("height", `${height}`);
      rect.setAttribute("rx", "7");
      rect.setAttribute("ry", "7");
      rect.setAttribute("fill", badgeColor);
      overlay.appendChild(rect);

      const text = createSvgElement("text");
      text.setAttribute("x", "0");
      text.setAttribute("y", `${y + height / 2}`);
      text.setAttribute("dy", "0.35em");
      text.setAttribute("text-anchor", "middle");
      text.setAttribute("fill", "#ffffff");
      text.setAttribute("font-size", "10");
      text.setAttribute("font-weight", "600");
      text.textContent = label;
      overlay.appendChild(text);
    }

    this.attachTooltip(halo, datum, annotation);
    this.attachTooltip(overlay, datum, annotation);
    node.appendChild(overlay);
  }

  private attachTooltip(
    element: Element,
    datum: D3Datum,
    annotation: AnnotationValue,
  ): void {
    const { title, rows } = tooltipLines(datum, annotation);
    element.addEventListener("mouseenter", (event) => {
      this.showTooltip(title, rows, event as MouseEvent);
    });
    element.addEventListener("mousemove", (event) => {
      this.positionTooltip(event as MouseEvent);
    });
    element.addEventListener("mouseleave", () => this.hideTooltip());
  }

  private ensureTooltip(): HTMLDivElement {
    if (!this.tooltip) {
      const tooltip = document.createElement("div");
      tooltip.className = "annotation-tooltip";
      Object.assign(tooltip.style, {
        position: "fixed",
        zIndex: "1000",
        pointerEvents: "none",
        background: "rgba(33, 37, 41, 0.96)",
        color: "#fff",
        padding: "6px 9px",
        borderRadius: "5px",
        fontSize: "12px",
        lineHeight: "1.4",
        maxWidth: "260px",
        boxShadow: "0 2px 8px rgba(0,0,0,0.25)",
        display: "none",
      } as Partial<CSSStyleDeclaration>);
      document.body.appendChild(tooltip);
      this.tooltip = tooltip;
    }
    return this.tooltip;
  }

  private showTooltip(title: string, rows: string[], event: MouseEvent): void {
    const tooltip = this.ensureTooltip();
    const body = rows
      .map((row) => `<div>${escapeHtml(row)}</div>`)
      .join("");
    tooltip.innerHTML = `<div style="font-weight:600;margin-bottom:2px">${escapeHtml(
      title,
    )}</div>${body}`;
    tooltip.style.display = "block";
    this.positionTooltip(event);
  }

  private positionTooltip(event: MouseEvent): void {
    if (!this.tooltip) {
      return;
    }
    const offset = 14;
    const { innerWidth, innerHeight } = window;
    const rect = this.tooltip.getBoundingClientRect();
    let left = event.clientX + offset;
    let top = event.clientY + offset;
    if (left + rect.width > innerWidth) {
      left = event.clientX - rect.width - offset;
    }
    if (top + rect.height > innerHeight) {
      top = event.clientY - rect.height - offset;
    }
    this.tooltip.style.left = `${Math.max(4, left)}px`;
    this.tooltip.style.top = `${Math.max(4, top)}px`;
  }

  private hideTooltip(): void {
    if (this.tooltip) {
      this.tooltip.style.display = "none";
    }
  }
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
