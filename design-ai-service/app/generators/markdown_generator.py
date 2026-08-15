import json
import re
from typing import Any


ROLE_PRIORITY = ("background", "text", "heading", "accent", "link", "border", "outline", "shadow", "unknown")


def _number(value: Any, fallback: float = 0) -> float:
    return float(value) if isinstance(value, (int, float)) else fallback


def _slug(value: Any, fallback: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", "-", str(value or "").lower())
    return normalized.strip("-") or fallback


def _unique_name(base: str, used: set[str]) -> str:
    candidate = base
    suffix = 2
    while candidate in used:
        candidate = f"{base}-{suffix}"
        suffix += 1
    used.add(candidate)
    return candidate


class MarkdownGenerator:
    """Build a compact design contract from already aggregated design data."""

    def generate(self, analysis_job_id: str, style: dict[str, Any], recommendations: list[str], confidence: float) -> str:
        manifest = self._build_manifest(analysis_job_id, style, recommendations, confidence)
        css = self._build_css(manifest["tokens"])
        lines = [
            "---",
            "document: ui-maker-agent-design-contract",
            "schemaVersion: 1",
            f"analysisId: {analysis_job_id}",
            f"pagesAnalyzed: {manifest['source']['pagesAnalyzed']}",
            f"confidence: {confidence:.2f}",
            "---",
            "",
            "# Agent Design Contract",
            "",
            "> Canonical, aggregated design data for implementation agents. Prefer these tokens over inventing new visual values.",
            "",
            "## Agent instructions",
            "",
            "1. Reuse a token when its semantic role matches the requested UI.",
            "2. Treat `value` as canonical and `evidence` as supporting context, not CSS to copy blindly.",
            "3. Preserve the spacing, radius, typography, and shadow scales before introducing a new value.",
            "4. When evidence conflicts, prefer higher `pageCoverage`, then higher `usageCount`.",
            "5. Do not infer missing behavior, accessibility states, or breakpoints from this file.",
            "",
            "## Machine-readable manifest",
            "",
            "```json",
            json.dumps(manifest, ensure_ascii=False, indent=2),
            "```",
            "",
            "## CSS token starter",
            "",
            "```css",
            css,
            "```",
            "",
            "## Implementation notes",
            "",
        ]
        lines.extend(f"- {item}" for item in recommendations)
        if not recommendations:
            lines.append("- No major inconsistency was detected in the aggregated token scales.")
        lines.extend([
            "",
            "## Scope and limitations",
            "",
            "- Values are ranked from cross-page aggregated evidence, not from a single DOM sample.",
            "- Low-signal values are intentionally limited to keep agent context compact.",
            "- Validate contrast, interaction states, and responsive behavior during implementation.",
            "",
        ])
        return "\n".join(lines)

    def _build_manifest(self, analysis_job_id: str, style: dict[str, Any], recommendations: list[str], confidence: float) -> dict[str, Any]:
        return {
            "schemaVersion": 1,
            "kind": "ui-maker.agent-design-contract",
            "analysisId": analysis_job_id,
            "confidence": round(confidence, 2),
            "source": {
                "pagesAnalyzed": style.get("metadata", {}).get("pageCount", 0),
                "aggregation": "cross-page-ranked",
            },
            "usagePolicy": {
                "tokenPreference": "semantic-role-first",
                "conflictResolution": ["pageCoverage", "usageCount"],
                "allowNewValues": "only-when-no-semantic-match",
            },
            "tokens": {
                "colors": self._color_tokens(style.get("colors", [])),
                "typography": self._typography_tokens(style.get("typography", [])),
                "spacing": self._scale_tokens("space", style.get("spacing", []), 12),
                "radii": self._scale_tokens("radius", style.get("radii", []), 8),
                "shadows": self._shadow_tokens(style.get("shadows", [])),
                "cssVariables": self._css_variables(style.get("cssVariables", [])),
            },
            "recommendations": recommendations,
        }

    def _color_tokens(self, colors: list[dict[str, Any]]) -> dict[str, Any]:
        ranked = sorted(colors, key=lambda item: (_number(item.get("prominenceScore")), _number(item.get("pageCoverage")), _number(item.get("usageCount"))), reverse=True)[:16]
        role_order = {role: index for index, role in enumerate(ROLE_PRIORITY)}
        ranked.sort(key=lambda item: role_order.get(item.get("role", "unknown"), 99))
        used: set[str] = set()
        result: dict[str, Any] = {}
        for item in ranked:
            role = _slug(item.get("role"), "unknown")
            name = _unique_name(f"color-{role}", used)
            result[name] = self._evidence(item, ("prominenceScore", "contexts", "elements"))
        return result

    def _typography_tokens(self, items: list[dict[str, Any]]) -> dict[str, Any]:
        ranked = sorted(items, key=lambda item: _number(item.get("usageCount")), reverse=True)[:10]
        return {
            f"type-{index}": {
                "fontFamily": item.get("fontFamily"),
                "fontSize": item.get("fontSize"),
                "fontWeight": item.get("fontWeight"),
                "lineHeight": item.get("lineHeight"),
                "letterSpacing": item.get("letterSpacing"),
                "evidence": self._evidence_counts(item),
            }
            for index, item in enumerate(ranked, start=1)
        }

    def _scale_tokens(self, prefix: str, items: list[dict[str, Any]], limit: int) -> dict[str, Any]:
        ranked = sorted(
            (item for item in items if item.get("value")),
            key=lambda item: (_number(item.get("pixels")), -_number(item.get("usageCount"))),
        )[:limit]
        return {
            f"{prefix}-{index}": self._evidence(item, ("pixels", "properties", "contexts", "corners"))
            for index, item in enumerate(ranked, start=1)
        }

    def _shadow_tokens(self, items: list[dict[str, Any]]) -> dict[str, Any]:
        ranked = sorted(items, key=lambda item: _number(item.get("usageCount")), reverse=True)[:6]
        return {
            f"shadow-{index}": self._evidence(item, ("contexts",))
            for index, item in enumerate(ranked, start=1) if item.get("value")
        }

    def _css_variables(self, items: list[dict[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for item in items[:40]:
            variants = item.get("variants") or []
            if not item.get("name") or not variants:
                continue
            best = max(variants, key=lambda variant: (_number(variant.get("pageCoverage")), _number(variant.get("pageCount"))))
            result[item["name"]] = {
                "value": best.get("value"),
                "evidence": {"pageCount": best.get("pageCount", 0), "pageCoverage": best.get("pageCoverage", 0)},
            }
        return result

    def _evidence(self, item: dict[str, Any], include: tuple[str, ...]) -> dict[str, Any]:
        output = {"value": item.get("value"), "evidence": self._evidence_counts(item)}
        for field in include:
            value = item.get(field)
            if value not in (None, [], {}):
                output["evidence"][field] = value
        return output

    def _evidence_counts(self, item: dict[str, Any]) -> dict[str, Any]:
        return {"usageCount": item.get("usageCount", 0), "pageCount": item.get("pageCount", 0), "pageCoverage": item.get("pageCoverage", 0)}

    def _build_css(self, tokens: dict[str, Any]) -> str:
        declarations: list[str] = []
        for group in ("colors", "spacing", "radii", "shadows"):
            for name, token in tokens[group].items():
                declarations.append(f"  --{name}: {token['value']};")
        for name, token in tokens["cssVariables"].items():
            css_name = name if str(name).startswith("--") else f"--{name}"
            declarations.append(f"  {css_name}: {token['value']};")
        return ":root {\n" + "\n".join(declarations) + "\n}"
