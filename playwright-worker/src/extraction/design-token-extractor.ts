import type { Page } from "playwright";

import type {
  ColorUsage,
  RadiusUsage,
  ShadowUsage,
  SpacingUsage,
  TypographyUsage,
} from "../contracts/crawl-request-event.js";

export interface ExtractedDesignTokens {
  cssVariables: Record<string, string>;
  colors: ColorUsage[];
  typography: TypographyUsage[];
  spacing: SpacingUsage[];
  radii: RadiusUsage[];
  shadows: ShadowUsage[];
}

interface DimensionAccumulator {
  pixels: number;
  usageCount: number;
  properties: Set<string>;
  contexts: Set<string>;
}

interface RadiusAccumulator {
  pixels: number | null;
  usageCount: number;
  corners: Set<string>;
  contexts: Set<string>;
}

interface ShadowAccumulator {
  usageCount: number;
  contexts: Set<string>;
}

export async function extractDesignTokens(
  page: Page
): Promise<ExtractedDesignTokens> {
  return page.evaluate(() => {
    const MAX_ELEMENTS = 5_000;
    const MAX_COLORS = 100;
    const MAX_TYPOGRAPHY = 100;
    const MAX_CSS_VARIABLES = 500;
    const MAX_SPACING = 100;
    const MAX_RADII = 100;
    const MAX_SHADOWS = 100;
    const MAX_CONTEXTS = 20;

    const cssVariables: Record<string, string> = {};

    const rootStyle = getComputedStyle(
      document.documentElement
    );
    const spacingMap = new Map<string, DimensionAccumulator>();
    const radiusMap = new Map<string, RadiusAccumulator>();
    const shadowMap = new Map<string, ShadowAccumulator>();

    function parsePixels(value: string): number | null {
      const match = value
        .trim()
        .match(/^(-?\d+(?:\.\d+)?)px$/);

      if (!match) {
        return null;
      }

      return Number(match[1]);
    }

    function normalizeCssValue(value: string): string {
      return value
        .trim()
        .toLowerCase()
        .replace(/\s+/g, " ");
    }

    function describeElement(element: HTMLElement): string {
      const tag = element.tagName.toLowerCase();
      const id = element.id ? `#${element.id}` : "";
      const classes = Array.from(element.classList)
        .slice(0, 2)
        .map((className) => `.${className}`)
        .join("");

      return `${tag}${id}${classes}`.slice(0, 120);
    }

    function addSpacing(
      value: string,
      property: string,
      context: string,
    ): void {
      const pixels = parsePixels(value);

      if (
        pixels === null ||
        !Number.isFinite(pixels) ||
        pixels <= 0
      ) {
        return;
      }

      const normalized = `${Math.round(pixels * 100) / 100}px`;

      const current = spacingMap.get(normalized) ?? {
        pixels,
        usageCount: 0,
        properties: new Set<string>(),
        contexts: new Set<string>(),
      };

      current.usageCount++;
      current.properties.add(property);

      if (current.contexts.size < MAX_CONTEXTS) {
        current.contexts.add(context);
      }

      spacingMap.set(normalized, current);
    }

    function addRadius(
      value: string,
      corner: string,
      context: string,
    ): void {
      const normalized = normalizeCssValue(value);

      if (!normalized) {
        return;
      }

      const numericParts = normalized
        .match(/-?\d+(?:\.\d+)?/g)
        ?.map(Number);

      if (
        numericParts?.length &&
        numericParts.every((part) => part === 0)
      ) {
        return;
      }

      const current = radiusMap.get(normalized) ?? {
        pixels: parsePixels(normalized),
        usageCount: 0,
        corners: new Set<string>(),
        contexts: new Set<string>(),
      };

      current.usageCount++;
      current.corners.add(corner);

      if (current.contexts.size < MAX_CONTEXTS) {
        current.contexts.add(context);
      }

      radiusMap.set(normalized, current);
    }

    function addShadow(
      value: string,
      context: string,
    ): void {
      const normalized = normalizeCssValue(value);

      if (!normalized || normalized === "none") {
        return;
      }

      const current = shadowMap.get(normalized) ?? {
        usageCount: 0,
        contexts: new Set<string>(),
      };

      current.usageCount++;

      if (current.contexts.size < MAX_CONTEXTS) {
        current.contexts.add(context);
      }

      shadowMap.set(normalized, current);
    }
    for (let index = 0; index < rootStyle.length; index++) {
      const property = rootStyle.item(index);

      if (!property.startsWith("--")) {
        continue;
      }

      const value = rootStyle
        .getPropertyValue(property)
        .trim();

      if (value) {
        cssVariables[property] = value;
      }

      if (
        Object.keys(cssVariables).length >=
        MAX_CSS_VARIABLES
      ) {
        break;
      }
    }

    interface ColorAccumulator {
      usageCount: number;
      visualArea: number;
      contexts: Set<string>;
      elements: Set<string>;
      roleCounts: Record<string, number>;
    }

    const colorMap = new Map<
      string,
      ColorAccumulator
    >();

    const typographyMap = new Map<
      string,
      {
        fontFamily: string;
        fontSize: string;
        fontWeight: string;
        lineHeight: string;
        letterSpacing: string;
        usageCount: number;
      }
    >();

    const ignoredColors = new Set(["transparent"]);

    function isTransparentColor(value: string): boolean {
      if (ignoredColors.has(value)) {
        return true;
      }

      const rgba = value.match(/^rgba\([^,]+,[^,]+,[^,]+,\s*([\d.]+)\)$/);
      return rgba !== null && Number(rgba[1]) === 0;
    }

    function extractColors(value: string): string[] {
      if (!value || value === "none") {
        return [];
      }

      return value.match(
        /#[0-9a-f]{3,8}\b|rgba?\([^)]+\)|hsla?\([^)]+\)/gi
      ) ?? [];
    }

    function visibleArea(rect: DOMRect): number {
      const width = Math.max(
        0,
        Math.min(rect.right, window.innerWidth) - Math.max(rect.left, 0)
      );
      const height = Math.max(
        0,
        Math.min(rect.bottom, window.innerHeight) - Math.max(rect.top, 0)
      );
      return Math.round(width * height);
    }

    function addColor(
      value: string,
      role: string,
      element: string,
      area = 0
    ): void {
      const normalized = normalizeCssValue(value);

      if (
        !normalized ||
        isTransparentColor(normalized)
      ) {
        return;
      }

      const current = colorMap.get(normalized) ?? {
        usageCount: 0,
        visualArea: 0,
        contexts: new Set<string>(),
        elements: new Set<string>(),
        roleCounts: {}
      };

      current.usageCount++;
      current.visualArea += Math.max(0, area);
      current.contexts.add(role);
      current.roleCounts[role] = (current.roleCounts[role] ?? 0) + 1;

      if (current.elements.size < MAX_CONTEXTS) {
        current.elements.add(element);
      }

      colorMap.set(normalized, current);
    }

    const elements = Array.from(
      document.querySelectorAll<HTMLElement>("html, body, body *")
    ).slice(0, MAX_ELEMENTS);

    for (const element of elements) {
      const rect = element.getBoundingClientRect();

      if (
        rect.width <= 0 ||
        rect.height <= 0
      ) {
        continue;
      }

      const style = getComputedStyle(element);

      if (
        style.display === "none" ||
        style.visibility === "hidden"
      ) {
        continue;
      }

      const context = describeElement(element);
      const area = visibleArea(rect);
      const hasDirectText = Array.from(element.childNodes).some(
        (node) => node.nodeType === Node.TEXT_NODE && Boolean(node.textContent?.trim())
      );
      const isControl = element.matches(
        "button, a, input[type='button'], input[type='submit'], [role='button']"
      );
      const parentBackground = element.parentElement
        ? getComputedStyle(element.parentElement).backgroundColor
        : "";

      if (hasDirectText) {
        const textRole = element.matches("h1, h2, h3, h4, h5, h6")
          ? "heading"
          : element.matches("a")
            ? "link"
            : "text";
        addColor(style.color, textRole, context);
      }

      // Avoid counting the same inherited/transparent surface for every child.
      if (normalizeCssValue(style.backgroundColor) !== normalizeCssValue(parentBackground)) {
        addColor(
          style.backgroundColor,
          isControl ? "control-background" : "background",
          context,
          area
        );
      }

      if (style.borderTopStyle !== "none" && parseFloat(style.borderTopWidth) > 0) {
        addColor(style.borderTopColor, "border-top", context);
      }
      if (style.borderRightStyle !== "none" && parseFloat(style.borderRightWidth) > 0) {
        addColor(style.borderRightColor, "border-right", context);
      }
      if (style.borderBottomStyle !== "none" && parseFloat(style.borderBottomWidth) > 0) {
        addColor(style.borderBottomColor, "border-bottom", context);
      }
      if (style.borderLeftStyle !== "none" && parseFloat(style.borderLeftWidth) > 0) {
        addColor(style.borderLeftColor, "border-left", context);
      }
      if (style.outlineStyle !== "none" && parseFloat(style.outlineWidth) > 0) {
        addColor(style.outlineColor, "outline", context);
      }

      for (const color of extractColors(style.boxShadow)) {
        addColor(color, "box-shadow", context);
      }

      for (const color of extractColors(style.textShadow)) {
        addColor(color, "text-shadow", context);
      }
      addSpacing(style.paddingTop, "padding-top", context);
      addSpacing(style.paddingRight, "padding-right", context);
      addSpacing(style.paddingBottom, "padding-bottom", context);
      addSpacing(style.paddingLeft, "padding-left", context);

      addSpacing(style.marginTop, "margin-top", context);
      addSpacing(style.marginRight, "margin-right", context);
      addSpacing(style.marginBottom, "margin-bottom", context);
      addSpacing(style.marginLeft, "margin-left", context);

      if (
        style.display.includes("flex") ||
        style.display.includes("grid")
      ) {
        addSpacing(style.rowGap, "row-gap", context);
        addSpacing(style.columnGap, "column-gap", context);
      }

      addRadius(
        style.borderTopLeftRadius,
        "top-left",
        context,
      );
      addRadius(
        style.borderTopRightRadius,
        "top-right",
        context,
      );
      addRadius(
        style.borderBottomRightRadius,
        "bottom-right",
        context,
      );
      addRadius(
        style.borderBottomLeftRadius,
        "bottom-left",
        context,
      );

      addShadow(style.boxShadow, context);

      const hasText =
        element.textContent?.trim().length;

      if (!hasText) {
        continue;
      }

      const typographyKey = [
        style.fontFamily,
        style.fontSize,
        style.fontWeight,
        style.lineHeight,
        style.letterSpacing
      ].join("|");

      const typography =
        typographyMap.get(typographyKey) ?? {
          fontFamily: style.fontFamily,
          fontSize: style.fontSize,
          fontWeight: style.fontWeight,
          lineHeight: style.lineHeight,
          letterSpacing: style.letterSpacing,
          usageCount: 0
        };

      typography.usageCount++;

      typographyMap.set(
        typographyKey,
        typography
      );
    }

    const colors = Array.from(
      colorMap.entries()
    )
      .map(([value, data]) => ({
        value,
        usageCount: data.usageCount,
        visualArea: data.visualArea,
        contexts: Array.from(data.contexts),
        elements: Array.from(data.elements),
        roleCounts: data.roleCounts
      }))
      .sort(
        (left, right) =>
          right.usageCount - left.usageCount
      )
      .slice(0, MAX_COLORS);

    const typography = Array.from(
      typographyMap.values()
    )
      .sort(
        (left, right) =>
          right.usageCount - left.usageCount
      )
      .slice(0, MAX_TYPOGRAPHY);

    const spacing = Array.from(spacingMap.entries())
      .map(([value, data]) => ({
        value,
        pixels: data.pixels,
        usageCount: data.usageCount,
        properties: Array.from(data.properties),
        contexts: Array.from(data.contexts),
      }))
      .sort((left, right) => right.usageCount - left.usageCount)
      .slice(0, MAX_SPACING);

    const radii = Array.from(radiusMap.entries())
      .map(([value, data]) => ({
        value,
        pixels: data.pixels,
        usageCount: data.usageCount,
        corners: Array.from(data.corners),
        contexts: Array.from(data.contexts),
      }))
      .sort((left, right) => right.usageCount - left.usageCount)
      .slice(0, MAX_RADII);

    const shadows = Array.from(shadowMap.entries())
      .map(([value, data]) => ({
        value,
        usageCount: data.usageCount,
        contexts: Array.from(data.contexts),
      }))
      .sort((left, right) => right.usageCount - left.usageCount)
      .slice(0, MAX_SHADOWS);

    return {
      cssVariables,
      colors,
      typography,
      spacing,
      radii,
      shadows,
    };
  });
}
