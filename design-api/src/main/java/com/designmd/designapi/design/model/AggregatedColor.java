package com.designmd.designapi.design.model;

import java.util.List;

public record AggregatedColor(
        String value,
        long usageCount,
        int pageCount,
        double pageCoverage,
        List<String> contexts,
        List<String> pageUrls
) {
}