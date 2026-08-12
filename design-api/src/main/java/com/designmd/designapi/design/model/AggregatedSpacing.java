package com.designmd.designapi.design.model;

import java.util.List;

public record AggregatedSpacing(
        String value,
        double pixels,
        long usageCount,
        int pageCount,
        double pageCoverage,
        List<String> properties,
        List<String> contexts,
        List<String> pageUrls
) {
}