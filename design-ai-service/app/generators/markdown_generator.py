import json
import re
from typing import Any


def _number(value: Any, fallback: float = 0) -> float:
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        try:
            return float(value)
        except ValueError:
            return fallback
    return fallback


def _px(value: Any) -> float:
    match = re.fullmatch(r"\s*(-?\d+(?:\.\d+)?)px\s*", str(value or ""))
    return float(match.group(1)) if match else 0


def _rgba(value: Any) -> tuple[int, int, int, float] | None:
    match = re.fullmatch(
        r"rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)",
        str(value or ""),
    )
    if match:
        return (
            int(match.group(1)),
            int(match.group(2)),
            int(match.group(3)),
            float(match.group(4) or 1),
        )

    hex_match = re.fullmatch(r"#([0-9a-fA-F]{6})", str(value or ""))
    if hex_match:
        raw = hex_match.group(1)
        return int(raw[0:2], 16), int(raw[2:4], 16), int(raw[4:6], 16), 1
    return None


def _is_primary_candidate(item: dict[str, Any]) -> bool:
    parsed = _rgba(item.get("value"))
    if not parsed:
        return item.get("role") in {"accent", "link"}
    red, green, blue, alpha = parsed
    chroma = max(red, green, blue) - min(red, green, blue)
    return alpha >= 0.8 and chroma >= 45


class MarkdownGenerator:
    """Generate a concise semantic DESIGN.md from aggregated evidence."""

    def generate(
            self,
            analysis_job_id: str,
            style: dict[str, Any],
            recommendations: list[str],
            confidence: float,
    ) -> str:
        tokens = self._tokens(style)
        metadata = style.get("metadata", {})
        page_count = metadata.get("pageCount", 0)
        effective_confidence = min(confidence, 0.65) if page_count < 2 else confidence

        contract = {
            "schemaVersion": 2,
            "kind": "ui-maker.design-contract",
            "analysisId": analysis_job_id,
            "confidence": round(effective_confidence, 2),
            "pagesAnalyzed": page_count,
            "tokens": tokens,
        }

        lines = [
            "---",
            "document: ui-maker-design-contract",
            "schemaVersion: 2",
            f"analysisId: {analysis_job_id}",
            f"pagesAnalyzed: {page_count}",
            f"confidence: {effective_confidence:.2f}",
            "---",
            "",
            "# DESIGN.md",
            "",
            "A compact design contract generated from aggregated computed styles.",
            "",
            "## Agent rules",
            "",
            "- Use the semantic tokens below before introducing new visual values.",
            "- Preserve token roles; do not choose values only because they look similar.",
            "- Treat missing component states, breakpoints, and accessibility behavior as unspecified.",
            "- Validate color contrast and responsive behavior in the implementation.",
            "",
            "## Canonical tokens",
            "",
            "```json",
            json.dumps(contract, ensure_ascii=False, indent=2),
            "```",
            "",
            "## Implementation guidance",
            "",
            f"- Base font: `{tokens['font'].get('sans', 'system-ui, sans-serif')}`.",
            "- Use spacing, radius, and shadow values as closed scales.",
            "- `color.primary` is the strongest opaque chromatic candidate; verify its intended action role.",
            "- Values from third-party widgets, ads, and low-frequency computed styles are excluded.",
        ]

        if recommendations:
            lines.extend(["", "## Review notes", ""])
            lines.extend(f"- {recommendation}" for recommendation in recommendations)

        if page_count < 2:
            lines.extend([
                "",
                "## Confidence note",
                "",
                "Only one page was analyzed. Cross-page consistency, responsive variants, and secondary layouts are not confirmed.",
            ])

        lines.append("")
        return "\n".join(lines)

    def _tokens(self, style: dict[str, Any]) -> dict[str, Any]:
        typography = self._typography(style.get("typography", []))
        return {
            "color": self._colors(style.get("colors", [])),
            "font": {"sans": typography.pop("fontFamily", "system-ui, sans-serif")},
            "typography": typography,
            "spacing": self._spacing(style.get("spacing", [])),
            "radius": self._radii(style.get("radii", [])),
            "shadow": self._shadows(style.get("shadows", [])),
        }

    def _colors(self, items: list[dict[str, Any]]) -> dict[str, str]:
        eligible = [item for item in items if item.get("value") and _number(item.get("usageCount")) >= 2]
        ranked = sorted(
            eligible,
            key=lambda item: (
                _number(item.get("prominenceScore")),
                _number(item.get("pageCoverage")),
                _number(item.get("usageCount")),
            ),
            reverse=True,
        )
        result: dict[str, str] = {}
        used: set[str] = set()

        def add(name: str, candidates: list[dict[str, Any]]) -> None:
            for candidate in candidates:
                value = str(candidate["value"])
                if value not in used:
                    result[name] = value
                    used.add(value)
                    return

        backgrounds = [item for item in ranked if item.get("role") == "background"]
        texts = [item for item in ranked if item.get("role") in {"text", "heading"}]
        borders = [item for item in ranked if item.get("role") == "border"]
        links = [item for item in ranked if item.get("role") == "link"]
        primary = [item for item in ranked if _is_primary_candidate(item)]

        add("canvas", backgrounds)
        add("surface", backgrounds)
        add("text", texts)
        add("textMuted", texts)
        add("primary", primary)
        add("link", links)
        add("border", borders)
        return result

    def _typography(self, items: list[dict[str, Any]]) -> dict[str, Any]:
        eligible = [item for item in items if item.get("fontFamily") and _number(item.get("usageCount")) >= 2]
        if not eligible:
            return {}
        ranked = sorted(eligible, key=lambda item: _number(item.get("usageCount")), reverse=True)
        body = ranked[0]
        body_size = _px(body.get("fontSize"))

        headings = [item for item in eligible if _px(item.get("fontSize")) > body_size or _number(item.get("fontWeight")) >= 600]
        labels = [item for item in ranked if _px(item.get("fontSize")) <= body_size and _number(item.get("fontWeight")) >= 500]
        captions = sorted(eligible, key=lambda item: (_px(item.get("fontSize")), -_number(item.get("usageCount"))))

        result: dict[str, Any] = {"fontFamily": body["fontFamily"], "body": self._type_style(body)}
        if headings:
            result["heading"] = self._type_style(max(headings, key=lambda item: (_px(item.get("fontSize")), _number(item.get("fontWeight")))))
        if labels:
            result["label"] = self._type_style(labels[0])
        if captions and _px(captions[0].get("fontSize")) < body_size:
            result["caption"] = self._type_style(captions[0])
        return result

    def _type_style(self, item: dict[str, Any]) -> dict[str, Any]:
        return {
            "fontSize": item.get("fontSize"),
            "fontWeight": item.get("fontWeight"),
            "lineHeight": item.get("lineHeight"),
            "letterSpacing": item.get("letterSpacing"),
        }

    def _spacing(self, items: list[dict[str, Any]]) -> dict[str, str]:
        by_pixels: dict[int, dict[str, Any]] = {}
        for item in items:
            pixels = _number(item.get("pixels"))
            rounded = round(pixels)
            if rounded < 2 or rounded > 96 or abs(pixels - rounded) > 0.2:
                continue
            existing = by_pixels.get(rounded)
            if not existing or _number(item.get("usageCount")) > _number(existing.get("usageCount")):
                by_pixels[rounded] = item

        strongest = sorted(by_pixels.items(), key=lambda pair: _number(pair[1].get("usageCount")), reverse=True)[:8]
        return {f"space-{pixels}": f"{pixels}px" for pixels, _ in sorted(strongest)}

    def _radii(self, items: list[dict[str, Any]]) -> dict[str, str]:
        result: dict[str, str] = {}
        numeric = [item for item in items if isinstance(item.get("pixels"), (int, float)) and _number(item.get("usageCount")) >= 3]
        strongest = sorted(numeric, key=lambda item: _number(item.get("usageCount")), reverse=True)[:5]
        for item in sorted(strongest, key=lambda item: _number(item.get("pixels"))):
            pixels = round(_number(item.get("pixels")))
            result[f"radius-{pixels}"] = f"{pixels}px"
        if any(item.get("value") == "50%" and _number(item.get("usageCount")) >= 2 for item in items):
            result["round"] = "50%"
        return result

    def _shadows(self, items: list[dict[str, Any]]) -> dict[str, str]:
        ranked = sorted(
            (item for item in items if item.get("value") and _number(item.get("usageCount")) >= 2),
            key=lambda item: _number(item.get("usageCount")),
            reverse=True,
        )[:2]
        return {f"shadow-{index}": str(item["value"]) for index, item in enumerate(ranked, start=1)}
