package com.designmd.designapi.crawl;

import com.designmd.designapi.messaging.event.ColorUsagePayload;
import com.designmd.designapi.messaging.event.RadiusUsagePayload;
import com.designmd.designapi.messaging.event.ShadowUsagePayload;
import com.designmd.designapi.messaging.event.SpacingUsagePayload;
import com.designmd.designapi.messaging.event.TypographyUsagePayload;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "crawled_pages")
@CompoundIndex(
        name = "analysis_page_unique",
        def = "{'analysisJobId': 1, 'pageUrl': 1}",
        unique = true
)
public class CrawledPage {

    @Id
    private String id;

    @Indexed
    private String analysisJobId;

    private String pageUrl;

    private String finalUrl;

    private String title;

    private long durationMs;

    private Instant createdAt;

    private Instant updatedAt;
    private Map<String, String> cssVariables;

    private List<ColorUsagePayload> colors;

    private List<TypographyUsagePayload> typography;

    private List<SpacingUsagePayload> spacing;

    private List<RadiusUsagePayload> radii;

    private List<ShadowUsagePayload> shadows;
}
