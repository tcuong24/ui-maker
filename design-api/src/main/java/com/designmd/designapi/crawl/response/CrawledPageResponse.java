package com.designmd.designapi.crawl.response;

import com.designmd.designapi.messaging.event.ColorUsagePayload;
import com.designmd.designapi.messaging.event.RadiusUsagePayload;
import com.designmd.designapi.messaging.event.ShadowUsagePayload;
import com.designmd.designapi.messaging.event.SpacingUsagePayload;
import com.designmd.designapi.messaging.event.TypographyUsagePayload;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CrawledPageResponse(
        String id,
        String pageUrl,
        String finalUrl,
        String title,
        long durationMs,
        Map<String, String> cssVariables,
        List<ColorUsagePayload> colors,
        List<TypographyUsagePayload> typography,
        List<SpacingUsagePayload> spacing,
        List<RadiusUsagePayload> radii,
        List<ShadowUsagePayload> shadows,
        Instant createdAt
) {
}
