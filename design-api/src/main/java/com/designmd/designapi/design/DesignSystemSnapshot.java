package com.designmd.designapi.design;

import com.designmd.designapi.design.model.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "design_system_snapshots")
public class DesignSystemSnapshot {

    @Id
    private String id;

    @Indexed(unique = true)
    private String analysisJobId;

    private int pageCount;

    private List<AggregatedColor> colors;
    private List<AggregatedTypography> typography;
    private List<AggregatedSpacing> spacing;
    private List<AggregatedRadius> radii;
    private List<AggregatedShadow> shadows;
    private List<AggregatedCssVariable> cssVariables;

    private Instant generatedAt;
    private Instant updatedAt;
}