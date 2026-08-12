package com.designmd.designapi.design.model;

import java.util.List;

public record AggregatedTypography(
        String fontFamily,
        String fontSize,
        String fontWeight,
        String lineHeight,
        String letterSpacing,
        long usageCount,
        int pageCount,
        double pageCoverage,
        List<String> pageUrls
) {
}