package com.designmd.designapi.messaging.event;

import java.time.Instant;

public record CrawlFailedEvent (String eventId,
                                int schemaVersion,
                                String sourceEventId,
                                String analysisJobId,
                                String errorCode,
                                String errorMessage,
                                Instant failedAt){
}
