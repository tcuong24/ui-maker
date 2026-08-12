package com.designmd.designapi.design.model;

import java.util.List;

public record AggregatedCssVariable(
        String name,
        List<CssVariableVariant> variants
) {
    public record CssVariableVariant(
            String value,
            int pageCount,
            double pageCoverage,
            List<String> pageUrls
    ) {
    }
}