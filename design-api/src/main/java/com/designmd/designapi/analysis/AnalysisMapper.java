package com.designmd.designapi.analysis;

import com.designmd.designapi.analysis.response.AnalysisDetailResponse;
import com.designmd.designapi.analysis.response.AnalysisSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnalysisMapper {
    AnalysisSummaryResponse toSummaryResponse(AnalysisJob job);

    AnalysisDetailResponse toDetailResponse(AnalysisJob job);
}
