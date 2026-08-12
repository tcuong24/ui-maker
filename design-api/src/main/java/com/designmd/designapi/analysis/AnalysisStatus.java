package com.designmd.designapi.analysis;


public enum AnalysisStatus {
    PENDING,
    CRAWLING,
    CRAWL_COMPLETED,
    ANALYZING,
    GENERATING_MARKDOWN,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean hasCrawlResult() {
        return switch (this) {
            case CRAWL_COMPLETED,
                 ANALYZING,
                 GENERATING_MARKDOWN,
                 COMPLETED -> true;

            default -> false;
        };
    }
}