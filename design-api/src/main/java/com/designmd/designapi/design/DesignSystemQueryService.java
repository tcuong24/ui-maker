package com.designmd.designapi.design;


import com.designmd.designapi.analysis.AnalysisJob;
import com.designmd.designapi.analysis.AnalysisJobRepository;
import com.designmd.designapi.analysis.AnalysisStatus;
import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DesignSystemQueryService {

    private final AnalysisJobRepository analysisJobRepository;
    private final DesignSystemRepository designSystemRepository;
    private final CurrentUserService currentUserService;

    public DesignSystemSnapshot getByAnalysisId(String analysisId) {
        String userId = currentUserService.getUserId();

        AnalysisJob job = analysisJobRepository
                .findByIdAndUserId(analysisId, userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.ANALYSIS_NOT_FOUND));

        if (job.getStatus() == AnalysisStatus.FAILED) {
            throw new AppException(ErrorCode.ANALYSIS_CRAWL_FAILED);
        }

        if (!job.getStatus().hasCrawlResult()) {
            throw new AppException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }

        return designSystemRepository
                .findByAnalysisJobId(analysisId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.ANALYSIS_NOT_COMPLETED));
    }
}