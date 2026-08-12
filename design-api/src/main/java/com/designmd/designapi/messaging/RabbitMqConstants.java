package com.designmd.designapi.messaging;

public final class RabbitMqConstants {

    public static final String EXCHANGE = "design.crawl.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "design.crawl.dlx";

    public static final String REQUESTED_QUEUE =
            "design.crawl.requested.queue";

    public static final String COMPLETED_QUEUE =
            "design.crawl.completed.queue";

    public static final String FAILED_QUEUE =
            "design.crawl.failed.queue";

    public static final String REQUESTED_DLQ =
            "design.crawl.requested.dlq";

    public static final String COMPLETED_DLQ =
            "design.crawl.completed.dlq";

    public static final String FAILED_DLQ =
            "design.crawl.failed.dlq";

    public static final String REQUESTED_KEY = "crawl.requested";
    public static final String COMPLETED_KEY = "crawl.completed";
    public static final String FAILED_KEY = "crawl.failed";

    public static final String REQUESTED_DEAD_KEY =
            "crawl.requested.dead";

    public static final String COMPLETED_DEAD_KEY =
            "crawl.completed.dead";

    public static final String FAILED_DEAD_KEY =
            "crawl.failed.dead";

    ///ai-analnize-event


    public static final String ANALYSIS_EXCHANGE =
            "design.analysis.exchange";

    public static final String ANALYSIS_DEAD_LETTER_EXCHANGE =
            "design.analysis.dlx";

    public static final String ANALYSIS_REQUESTED_QUEUE =
            "design.analysis.requested.queue";

    public static final String ANALYSIS_REQUESTED_DLQ =
            "design.analysis.requested.dlq";

    public static final String ANALYSIS_REQUESTED_KEY =
            "analysis.requested";

    public static final String ANALYSIS_COMPLETED_KEY =
            "analysis.completed";

    public static final String ANALYSIS_FAILED_KEY =
            "analysis.failed";

    public static final String ANALYSIS_REQUESTED_DEAD_KEY =
            "analysis.requested.dead";

    public static final String ANALYSIS_COMPLETED_QUEUE =
            "design.analysis.completed.queue";

    public static final String ANALYSIS_COMPLETED_DLQ =
            "design.analysis.completed.dlq";

    public static final String ANALYSIS_COMPLETED_DEAD_KEY =
            "analysis.completed.dead";

    private RabbitMqConstants() {
    }
}
