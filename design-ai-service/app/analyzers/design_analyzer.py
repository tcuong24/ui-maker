from typing import Any

from app.contracts.events import DesignAnalysisRequestedEvent


def pick(
        item: dict[str, Any],
        fields: tuple[str, ...],
) -> dict[str, Any]:
    return {
        field: item.get(field)
        for field in fields
        if field in item
    }


class DesignAnalyzer:
    def analyze(
            self,
            event: DesignAnalysisRequestedEvent,
    ) -> dict[str, Any]:
        colors = [
            pick(
                item,
                (
                    "value",
                    "usageCount",
                    "visualArea",
                    "pageCount",
                    "pageCoverage",
                    "prominenceScore",
                    "role",
                    "contexts",
                    "elements",
                    "roleCounts",
                ),
            )
            for item in event.colors[:30]
        ]

        typography = [
            pick(
                item,
                (
                    "fontFamily",
                    "fontSize",
                    "fontWeight",
                    "lineHeight",
                    "letterSpacing",
                    "usageCount",
                    "pageCount",
                    "pageCoverage",
                ),
            )
            for item in event.typography[:20]
        ]

        spacing = [
            pick(
                item,
                (
                    "value",
                    "pixels",
                    "usageCount",
                    "pageCount",
                    "pageCoverage",
                    "properties",
                ),
            )
            for item in event.spacing
            if (
                    isinstance(item.get("pixels"), (int, float))
                    and 0 < item["pixels"] <= 128
            )
        ][:20]

        radii = [
            pick(
                item,
                (
                    "value",
                    "pixels",
                    "usageCount",
                    "pageCount",
                    "pageCoverage",
                    "corners",
                ),
            )
            for item in event.radii[:10]
        ]

        shadows = [
            pick(
                item,
                (
                    "value",
                    "usageCount",
                    "pageCount",
                    "pageCoverage",
                ),
            )
            for item in event.shadows[:10]
        ]

        css_variables = [
            {
                "name": item.get("name"),
                "variants": [
                    pick(
                        variant,
                        (
                            "value",
                            "pageCount",
                            "pageCoverage",
                        ),
                    )
                    for variant in item.get("variants", [])
                ],
            }
            for item in event.css_variables[:100]
        ]

        recommendations = self._recommend(
            spacing=spacing,
            radii=radii,
            typography=typography,
        )

        confidence = self._confidence(
            page_count=event.page_count,
            color_count=len(colors),
            typography_count=len(typography),
        )

        return {
            "style": {
                "metadata": {
                    "analysisJobId": event.analysis_job_id,
                    "pageCount": event.page_count,
                },
                "colors": colors,
                "typography": typography,
                "spacing": spacing,
                "radii": radii,
                "shadows": shadows,
                "cssVariables": css_variables,
            },
            "recommendations": recommendations,
            "confidence": confidence,
        }

    def _recommend(
            self,
            spacing: list[dict[str, Any]],
            radii: list[dict[str, Any]],
            typography: list[dict[str, Any]],
    ) -> list[str]:
        recommendations: list[str] = []

        if len(spacing) > 12:
            recommendations.append(
                "Spacing scale contains many values; "
                "consider reducing it to a smaller reusable scale."
            )

        radius_values = {
            item.get("value")
            for item in radii
            if item.get("value")
        }

        if len(radius_values) > 5:
            recommendations.append(
                "Too many radius values were detected; "
                "consider standardizing component corners."
            )

        font_families = {
            item.get("fontFamily")
            for item in typography
            if item.get("fontFamily")
        }

        if len(font_families) > 3:
            recommendations.append(
                "Multiple font families were detected; "
                "consider defining primary and monospace families."
            )

        return recommendations

    def _confidence(
            self,
            page_count: int,
            color_count: int,
            typography_count: int,
    ) -> float:
        score = 0.4

        if page_count >= 2:
            score += 0.2

        if color_count >= 3:
            score += 0.2

        if typography_count >= 2:
            score += 0.2

        return min(round(score, 2), 1.0)
