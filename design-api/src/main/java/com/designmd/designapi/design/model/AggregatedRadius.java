package com.designmd.designapi.design.model;

import java.util.List;

public record AggregatedRadius(
        String value,
        Double pixels,
        long usageCount,
        int pageCount,
        double pageCoverage,
        List<String> corners,
        List<String> contexts,
        List<String> pageUrls
) {
}