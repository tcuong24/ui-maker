package com.designmd.designapi.analysis.response;

import com.designmd.designapi.analysis.AnalysisStatus;

import java.time.Instant;
import java.util.List;

public record AnalysisDetailResponse (String id,
                                      String websiteUrl,
                                      List<String> additionalPaths,
                                      boolean includeScreenshot,
                                      AnalysisStatus status,
                                      int progress,
                                      String errorCode,
                                      String errorMessage,
                                      Instant createdAt,
                                      Instant updatedAt,
                                      Instant completedAt){
}
