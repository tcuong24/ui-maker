package com.designmd.designapi.messaging.event;

import java.time.Instant;
import java.util.List;

public record CrawlCompletedEvent (String eventId,
                                   int schemaVersion,
                                   String sourceEventId,
                                   String analysisJobId,
                                   List<CrawledPagePayload> pages,
                                   Instant startedAt,
                                   Instant completedAt){
}
