package com.designmd.designapi.analysis.response;

import com.designmd.designapi.analysis.AnalysisStatus;

public record AnalysisCreatedResponse (String id,
                                       String websiteUrl,
                                       AnalysisStatus status,
                                       int progress) {
}
