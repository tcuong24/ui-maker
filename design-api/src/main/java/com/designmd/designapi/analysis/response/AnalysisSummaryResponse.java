package com.designmd.designapi.analysis.response;

import com.designmd.designapi.analysis.AnalysisStatus;

import java.time.Instant;

public record AnalysisSummaryResponse (String id,
                                      String websiteUrl,
                                      AnalysisStatus status,
                                      int progress,
                                      Instant createdAt,
                                      Instant updatedAt){
}
