from typing import Any


def escape_markdown(value: Any) -> str:
    return (
        str(value if value is not None else "")
        .replace("|", "\\|")
        .replace("\n", " ")
    )


class MarkdownGenerator:
    def generate(
            self,
            analysis_job_id: str,
            style: dict[str, Any],
            recommendations: list[str],
            confidence: float,
    ) -> str:
        lines: list[str] = [
            "# Design System Report",
            "",
            f"Analysis ID: `{analysis_job_id}`",
            "",
            f"Confidence: `{confidence:.2f}`",
            "",
            "## Summary",
            "",
            f"- Pages analyzed: {style['metadata']['pageCount']}",
            f"- Colors: {len(style['colors'])}",
            f"- Typography styles: {len(style['typography'])}",
            f"- Spacing tokens: {len(style['spacing'])}",
            f"- Radius tokens: {len(style['radii'])}",
            f"- Shadow tokens: {len(style['shadows'])}",
            "",
        ]

        self._append_colors(lines, style["colors"])
        self._append_typography(lines, style["typography"])
        self._append_spacing(lines, style["spacing"])
        self._append_radii(lines, style["radii"])
        self._append_shadows(lines, style["shadows"])

        lines.extend([
            "## Recommendations",
            "",
        ])

        if recommendations:
            for recommendation in recommendations:
                lines.append(f"- {escape_markdown(recommendation)}")
        else:
            lines.append("- No major inconsistency detected.")

        lines.append("")

        return "\n".join(lines)

    def _append_colors(
            self,
            lines: list[str],
            colors: list[dict[str, Any]],
    ) -> None:
        lines.extend([
            "## Colors",
            "",
            "| Value | Usage | Coverage | Contexts |",
            "|---|---:|---:|---|",
        ])

        for color in colors:
            contexts = ", ".join(color.get("contexts", []))

            lines.append(
                "| "
                f"`{escape_markdown(color.get('value'))}` | "
                f"{color.get('usageCount', 0)} | "
                f"{color.get('pageCoverage', 0):.0%} | "
                f"{escape_markdown(contexts)} |"
            )

        lines.append("")

    def _append_typography(
            self,
            lines: list[str],
            typography: list[dict[str, Any]],
    ) -> None:
        lines.extend([
            "## Typography",
            "",
            "| Font | Size | Weight | Line height | Usage |",
            "|---|---:|---:|---:|---:|",
        ])

        for item in typography:
            lines.append(
                "| "
                f"{escape_markdown(item.get('fontFamily'))} | "
                f"{escape_markdown(item.get('fontSize'))} | "
                f"{escape_markdown(item.get('fontWeight'))} | "
                f"{escape_markdown(item.get('lineHeight'))} | "
                f"{item.get('usageCount', 0)} |"
            )

        lines.append("")

    def _append_spacing(
            self,
            lines: list[str],
            spacing: list[dict[str, Any]],
    ) -> None:
        lines.extend([
            "## Spacing",
            "",
            "| Value | Pixels | Usage | Coverage |",
            "|---|---:|---:|---:|",
        ])

        for item in spacing:
            lines.append(
                "| "
                f"`{escape_markdown(item.get('value'))}` | "
                f"{item.get('pixels', 0)} | "
                f"{item.get('usageCount', 0)} | "
                f"{item.get('pageCoverage', 0):.0%} |"
            )

        lines.append("")

    def _append_radii(
            self,
            lines: list[str],
            radii: list[dict[str, Any]],
    ) -> None:
        lines.extend([
            "## Border Radius",
            "",
            "| Value | Usage | Coverage |",
            "|---|---:|---:|",
        ])

        for item in radii:
            lines.append(
                "| "
                f"`{escape_markdown(item.get('value'))}` | "
                f"{item.get('usageCount', 0)} | "
                f"{item.get('pageCoverage', 0):.0%} |"
            )

        lines.append("")

    def _append_shadows(
            self,
            lines: list[str],
            shadows: list[dict[str, Any]],
    ) -> None:
        lines.extend([
            "## Shadows",
            "",
            "| Value | Usage | Coverage |",
            "|---|---:|---:|",
        ])

        for item in shadows:
            lines.append(
                "| "
                f"`{escape_markdown(item.get('value'))}` | "
                f"{item.get('usageCount', 0)} | "
                f"{item.get('pageCoverage', 0):.0%} |"
            )

        lines.append("")