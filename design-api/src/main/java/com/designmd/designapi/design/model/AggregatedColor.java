package com.designmd.designapi.design.model;

import java.util.List;
import java.util.Map;

public record AggregatedColor(
        String value,
        long usageCount,
        double visualArea,
        int pageCount,
        double pageCoverage,
        double prominenceScore,
        String role,
        List<String> contexts,
        List<String> elements,
        Map<String, Long> roleCounts,
        List<String> pageUrls
) {
}
