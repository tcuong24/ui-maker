package com.designmd.designapi.crawl.request;

import java.time.Instant;
import java.util.List;

public record CrawlRequestedEvent(String eventId,
                                  int schemaVersion,
                                  String analysisJobId,
                                  String websiteUrl,
                                  List<String> additionalPaths,
                                  boolean includeScreenshot,
                                  Instant occurredAt) {

}
