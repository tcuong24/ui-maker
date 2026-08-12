package com.designmd.designapi.ai.event;

import java.time.Instant;
import java.util.Map;

public record DesignAnalysisCompletedEvent(String eventId,
                                           int schemaVersion,
                                           String sourceEventId,
                                           String analysisJobId,
                                           Map<String, Object> style,
                                           String markdownContent,
                                           double confidence,
                                           Instant completedAt) {
}
