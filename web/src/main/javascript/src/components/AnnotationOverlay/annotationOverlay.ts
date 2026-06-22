import {
  AnnotationMap,
  AnnotationValue,
  formatNumber,
} from "../../shared/annotations";

const SVG_NS = "http://www.w3.org/2000/svg";
const OVERLAY_CLASS = "annotation-overlay";
const HALO_CLASS = "annotation-halo";
// Marks tooltip rows we inject into the tree library's own node tooltip.
const TOOLTIP_ITEM_CLASS = "annotation-tooltip-item";
// Readable badge: white pill, neutral border, dark bold number.
const BADGE_BG = "#ffffff";
const BADGE_BORDER = "#adb5bd";
const BADGE_BORDER_HOVER = "#495057";
const BADGE_TEXT = "#212529";
const REAPPLY_DEBOUNCE_MS = 60;

type DataNode = {
  code?: string;
  name?: string;
  children?: Record<string, DataNode>;
};

type D3Datum = {
  data?: DataNode;
  // Visible children, and (when collapsed) hidden children, set by the library.
  children?: unknown;
  _children?: unknown;
};

/**
 * What a single rendered node shows. For a collapsed node this rolls up its own
 * annotation with every hidden descendant's annotation.
 */
type EffectiveAnnotation = {
  value?: number;
  genes?: string[];
  label?: string;
  /** True when descendant annotations were folded in. */
  rolledUp: boolean;
  /** Number of annotated codes that contributed (own + descendants). */
  contributors: number;
};

function getDatum(element: Element): D3Datum | undefined {
  return (element as unknown as { __data__?: D3Datum }).__data__;
}

function createSvgElement<K extends keyof SVGElementTagNameMap>(
  tag: K,
): SVGElementTagNameMap[K] {
  return document.createElementNS(SVG_NS, tag);
}

/** Rendered width of a node's label, to place the badge just after it. */
function nodeLabelWidth(node: SVGGElement): number {
  const text =
    node.querySelector<SVGTextElement>("text.nodeText") ??
    node.querySelector<SVGTextElement>("text");
  try {
    return text ? text.getBBox().width : 0;
  } catch {
    return 0;
  }
}

/** Codes of every descendant of a node (the full original subtree). */
function collectDescendantCodes(node: DataNode): string[] {
  const codes: string[] = [];
  const stack: DataNode[] = Object.values(node.children ?? {});
  while (stack.length > 0) {
    const current = stack.pop() as DataNode;
    if (current.code) {
      codes.push(current.code.toUpperCase());
    }
    for (const child of Object.values(current.children ?? {})) {
      stack.push(child);
    }
  }
  return codes;
}

/** Sum numeric values and union gene lists across annotations. */
function aggregate(annotations: AnnotationValue[]): {
  value?: number;
  genes?: string[];
} {
  let value: number | undefined;
  const geneSet = new Set<string>();
  for (const annotation of annotations) {
    if (typeof annotation.value === "number") {
      value = (value ?? 0) + annotation.value;
    }
    annotation.genes?.forEach((gene) => geneSet.add(gene));
  }
  return { value, genes: geneSet.size > 0 ? [...geneSet] : undefined };
}

/** Short text shown in the on-node badge. */
function badgeText(effective: EffectiveAnnotation): string | null {
  if (effective.value !== undefined) {
    return formatNumber(effective.value);
  }
  if (effective.genes && effective.genes.length > 0) {
    return `${effective.genes.length} gene${effective.genes.length === 1 ? "" : "s"}`;
  }
  if (effective.label) {
    return effective.label.length > 16
      ? `${effective.label.slice(0, 15)}…`
      : effective.label;
  }
  return null;
}

/**
 * Decorates the OncoTree SVG with annotation badges and folds the annotation
 * detail into the tree library's own node tooltip. The library re-renders nodes
 * (with d3 transitions) on every expand/collapse and re-applies its own styling,
 * so badges are appended as extra children of each `g.node` and re-applied
 * whenever the SVG mutates.
 */
export default class AnnotationOverlay {
  private container: HTMLElement;
  private annotations: AnnotationMap = {};
  private observer: MutationObserver;
  private tooltipObserver: MutationObserver;
  private reapplyTimer: number | undefined;

  constructor(container: HTMLElement) {
    this.container = container;
    this.observer = new MutationObserver(() => this.scheduleReapply());
    this.tooltipObserver = new MutationObserver(() => this.augmentTooltip());
  }

  setAnnotations(annotations: AnnotationMap | null): void {
    this.annotations = annotations ?? {};
    this.apply();
    if (Object.keys(this.annotations).length > 0) {
      this.startObserving();
      this.observeTooltip();
    } else {
      this.observer.disconnect();
      this.tooltipObserver.disconnect();
    }
  }

  destroy(): void {
    this.observer.disconnect();
    this.tooltipObserver.disconnect();
    if (this.reapplyTimer !== undefined) {
      window.clearTimeout(this.reapplyTimer);
    }
    this.clearOverlays();
    this.getTooltipContainer()
      ?.querySelectorAll(`.${TOOLTIP_ITEM_CLASS}`)
      .forEach((element) => element.remove());
  }

  private getSvgGroup(): SVGGElement | null {
    return this.container.querySelector("svg g");
  }

  /** The library appends an absolutely-positioned div to the container for its tooltip. */
  private getTooltipContainer(): HTMLElement | null {
    const divs = this.container.querySelectorAll<HTMLElement>(":scope > div");
    for (const div of divs) {
      if (div.style.position === "absolute") {
        return div;
      }
    }
    return null;
  }

  private startObserving(): void {
    const group = this.getSvgGroup();
    if (group) {
      this.observer.observe(group, { childList: true, subtree: true });
    }
  }

  private observeTooltip(): void {
    const tooltip = this.getTooltipContainer();
    if (tooltip) {
      this.tooltipObserver.observe(tooltip, { childList: true, subtree: true });
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
      this.startObserving();
      this.observeTooltip();
    }
  }

  private decorateNode(node: SVGGElement): void {
    const datum = getDatum(node);
    const code = datum?.data?.code?.toUpperCase();
    if (!datum || !datum.data || !code) {
      return;
    }

    const effective = this.effectiveAnnotation(datum, code);
    if (!effective) {
      return;
    }
    const label = badgeText(effective);
    if (!label) {
      return;
    }

    const overlay = createSvgElement("g");
    overlay.setAttribute("class", OVERLAY_CLASS);
    overlay.style.pointerEvents = "all";
    overlay.style.cursor = "pointer";

    const paddingX = 6;
    const charWidth = 6.6;
    const height = 16;
    const width = Math.max(18, label.length * charWidth + paddingX * 2);

    // Sit on the node's own row (vertically centered) to the right of its label,
    // so badges never overlap neighbouring rows. Leaf labels render to the right
    // of the node, parent labels to the left.
    const hasChildren = !!datum.children || !!datum._children;
    const left = hasChildren ? 12 : 14 + nodeLabelWidth(node);
    const y = -height / 2;

    const rect = createSvgElement("rect");
    rect.setAttribute("x", `${left}`);
    rect.setAttribute("y", `${y}`);
    rect.setAttribute("width", `${width}`);
    rect.setAttribute("height", `${height}`);
    rect.setAttribute("rx", "8");
    rect.setAttribute("ry", "8");
    rect.setAttribute("fill", BADGE_BG);
    rect.setAttribute("stroke", BADGE_BORDER);
    rect.setAttribute("stroke-width", "1");
    overlay.appendChild(rect);

    const text = createSvgElement("text");
    text.setAttribute("x", `${left + width / 2}`);
    text.setAttribute("y", "0");
    text.setAttribute("dy", "0.35em");
    text.setAttribute("text-anchor", "middle");
    text.setAttribute("fill", BADGE_TEXT);
    text.setAttribute("font-size", "11");
    text.setAttribute("font-weight", "700");
    text.textContent = label;
    overlay.appendChild(text);

    // The badge is part of the node: hovering it shows the node tooltip (the
    // library only wires this to the label), and clicking it toggles the node.
    const nodeText =
      node.querySelector<SVGTextElement>("text.nodeText") ??
      node.querySelector<SVGTextElement>("text");
    const nodeCircle = node.querySelector<SVGCircleElement>("circle.nodeCircle");
    overlay.addEventListener("mouseenter", (event) => {
      rect.setAttribute("stroke", BADGE_BORDER_HOVER);
      nodeText?.dispatchEvent(
        new MouseEvent("mouseover", {
          bubbles: true,
          clientX: event.clientX,
          clientY: event.clientY,
        }),
      );
    });
    overlay.addEventListener("mouseleave", (event) => {
      rect.setAttribute("stroke", BADGE_BORDER);
      nodeText?.dispatchEvent(
        new MouseEvent("mouseout", {
          bubbles: true,
          clientX: event.clientX,
          clientY: event.clientY,
        }),
      );
    });
    overlay.addEventListener("click", () => {
      if (hasChildren) {
        // Parent badge: toggle expand/collapse like the node itself.
        nodeCircle?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      } else if (window.parent !== window) {
        // Leaf badge: let an embedding host react (e.g. filter to this code).
        window.parent.postMessage(
          {
            type: "oncotree-node-click",
            code,
            label: datum.data?.name,
          },
          "*",
        );
      }
    });

    node.appendChild(overlay);
  }

  /**
   * The annotation a node should display. A collapsed node folds in its own
   * annotation plus every hidden descendant's (sum values, union genes); an
   * expanded node shows only its own, since its descendants render themselves.
   */
  private effectiveAnnotation(
    datum: D3Datum,
    code: string,
  ): EffectiveAnnotation | null {
    const own = this.annotations[code];
    const isCollapsed = !!datum._children;

    const descendantAnnotations = isCollapsed
      ? collectDescendantCodes(datum.data as DataNode)
          .map((descendantCode) => this.annotations[descendantCode])
          .filter((annotation): annotation is AnnotationValue => !!annotation)
      : [];

    if (!own && descendantAnnotations.length === 0) {
      return null;
    }

    const all = own ? [own, ...descendantAnnotations] : descendantAnnotations;
    const merged = aggregate(all);
    const rolledUp = descendantAnnotations.length > 0;

    return {
      value: merged.value,
      genes: merged.genes,
      label: rolledUp ? undefined : own?.label,
      rolledUp,
      contributors: (own ? 1 : 0) + descendantAnnotations.length,
    };
  }

  /**
   * Tooltip rows for a code. An expanded node shows its own value (and, if it
   * has annotated descendants, a "subtree total" line clarifying that the badge
   * is this node only and collapsing rolls the descendants up). A collapsed node
   * shows the rolled-up sum/union.
   */
  private buildTooltipRows(code: string): string[] {
    const node = Array.from(
      this.container.querySelectorAll<SVGGElement>("g.node"),
    ).find((element) => getDatum(element)?.data?.code?.toUpperCase() === code);
    const datum = node ? getDatum(node) : undefined;
    const own = this.annotations[code];

    const descendantAnnotations = datum?.data
      ? collectDescendantCodes(datum.data)
          .map((descendantCode) => this.annotations[descendantCode])
          .filter((annotation): annotation is AnnotationValue => !!annotation)
      : [];

    if (!own && descendantAnnotations.length === 0) {
      return [];
    }

    const collapsed = !!datum?._children && descendantAnnotations.length > 0;
    const subtree = aggregate(
      own ? [own, ...descendantAnnotations] : descendantAnnotations,
    );
    const totalNodes = (own ? 1 : 0) + descendantAnnotations.length;
    const rows: string[] = [];

    if (collapsed) {
      if (subtree.value !== undefined) {
        rows.push(`Sum: ${formatNumber(subtree.value)}`);
      }
      if (subtree.genes?.length) {
        rows.push(
          `Genes (union) (${subtree.genes.length}): ${subtree.genes.join(", ")}`,
        );
      }
      rows.push(
        `Aggregated from ${totalNodes} node${totalNodes === 1 ? "" : "s"} — expand to see each`,
      );
      return rows;
    }

    // Expanded (or leaf): show this node's own annotation.
    if (own?.value !== undefined) {
      rows.push(`Value: ${formatNumber(own.value)}`);
    } else if (own?.label) {
      rows.push(`Annotation: ${own.label}`);
    }
    if (own?.genes?.length) {
      rows.push(`Genes (${own.genes.length}): ${own.genes.join(", ")}`);
    }
    if (descendantAnnotations.length > 0) {
      const total =
        subtree.value !== undefined ? `: ${formatNumber(subtree.value)}` : "";
      rows.push(
        `Subtree total${total} across ${totalNodes} annotated node${
          totalNodes === 1 ? "" : "s"
        } (collapse to combine)`,
      );
    }
    return rows;
  }

  /** Append annotation rows to the library's node tooltip when it appears. */
  private augmentTooltip(): void {
    const container = this.getTooltipContainer();
    const inner = container?.querySelector(".oncotree-tooltip");
    if (!inner || inner.querySelector(`.${TOOLTIP_ITEM_CLASS}`)) {
      return;
    }
    const codeItem = Array.from(
      inner.querySelectorAll(".oncotree-tooltip-item"),
    )
      .map((element) => element.textContent ?? "")
      .find((text) => text.toLowerCase().startsWith("code:"));
    if (!codeItem) {
      return;
    }
    const code = codeItem.slice(codeItem.indexOf(":") + 1).trim().toUpperCase();
    for (const row of this.buildTooltipRows(code)) {
      const item = document.createElement("div");
      item.className = `oncotree-tooltip-item ${TOOLTIP_ITEM_CLASS}`;
      item.textContent = row;
      inner.appendChild(item);
    }
  }
}
