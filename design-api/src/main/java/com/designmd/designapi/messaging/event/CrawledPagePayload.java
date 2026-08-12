package com.designmd.designapi.messaging.event;

import java.util.List;
import java.util.Map;

public record CrawledPagePayload(
        String url,
        String finalUrl,
        String title,
        long durationMs,
        Map<String, String> cssVariables,
        List<ColorUsagePayload> colors,
        List<TypographyUsagePayload> typography,
        List<SpacingUsagePayload> spacing,
        List<RadiusUsagePayload> radii,
        List<ShadowUsagePayload> shadows
) {
}