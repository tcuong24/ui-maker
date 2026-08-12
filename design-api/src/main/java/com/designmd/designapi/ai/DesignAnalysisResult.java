package com.designmd.designapi.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "design_analysis_results")
public class DesignAnalysisResult {

    @Id
    private String id;

    @Indexed(unique = true)
    private String analysisJobId;

    private Map<String, Object> style;

    private String markdownContent;

    private double confidence;

    private String sourceEventId;

    private Instant completedAt;

    private Instant createdAt;

    private Instant updatedAt;
}