package com.designmd.designapi.ai.event;

import com.designmd.designapi.design.model.*;

import java.time.Instant;
import java.util.List;

public record DesignAnalysisRequestedEvent(String eventId,
                                           int schemaVersion,
                                           String analysisJobId,
                                           int pageCount,
                                           List<AggregatedColor> colors,
                                           List<AggregatedTypography> typography,
                                           List<AggregatedSpacing> spacing,
                                           List<AggregatedRadius> radii,
                                           List<AggregatedShadow> shadows,
                                           List<AggregatedCssVariable> cssVariables,
                                           Instant occurredAt) {
}
