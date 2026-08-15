package com.designmd.designapi.analysis;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document(collection = "analysis_jobs")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AnalysisJob {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String websiteUrl;

    @Indexed
    private String normalizedUrl;

    @Indexed
    private String domain;

    @Indexed
    private String cacheKey;

    private List<String> additionalPaths;

    private boolean includeScreenshot;

    private AnalysisStatus status;

    private int progress;

    private String errorCode;

    private String errorMessage;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;

    private boolean cacheHit;

    private String sourceAnalysisId;
    @Version
    Long version;
}

