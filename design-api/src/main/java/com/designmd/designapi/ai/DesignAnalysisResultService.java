package com.designmd.designapi.ai;

import com.designmd.designapi.ai.event.DesignAnalysisCompletedEvent;
import com.designmd.designapi.analysis.AnalysisJob;
import com.designmd.designapi.analysis.AnalysisJobRepository;
import com.designmd.designapi.analysis.AnalysisStatus;
import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DesignAnalysisResultService {

    private final DesignAnalysisResultRepository resultRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final CurrentUserService currentUserService;

    public void handleCompleted(
            DesignAnalysisCompletedEvent event
    ) {
        AnalysisJob job = analysisJobRepository
                .findById(event.analysisJobId())
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ANALYSIS_NOT_FOUND
                        ));

        Instant now = Instant.now();

        DesignAnalysisResult result = resultRepository
                .findByAnalysisJobId(event.analysisJobId())
                .orElseGet(() ->
                        DesignAnalysisResult.builder()
                                .analysisJobId(
                                        event.analysisJobId()
                                )
                                .createdAt(now)
                                .build()
                );

        result.setStyle(event.style());
        result.setMarkdownContent(event.markdownContent());
        result.setConfidence(event.confidence());
        result.setSourceEventId(event.sourceEventId());
        result.setCompletedAt(event.completedAt());
        result.setUpdatedAt(now);

        resultRepository.save(result);

        job.setStatus(AnalysisStatus.COMPLETED);
        job.setProgress(100);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        job.setCompletedAt(event.completedAt());
        job.setUpdatedAt(now);

        analysisJobRepository.save(job);
    }

    public Map<String, Object> getStyle(
            String analysisId
    ) {
        verifyOwnership(analysisId);

        return findResult(analysisId).getStyle();
    }

    public String getMarkdown(
            String analysisId
    ) {
        verifyOwnership(analysisId);

        return findResult(analysisId)
                .getMarkdownContent();
    }

    private DesignAnalysisResult findResult(
            String analysisId
    ) {
        return resultRepository
                .findByAnalysisJobId(analysisId)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ANALYSIS_RESULT_NOT_READY
                        ));
    }

    private void verifyOwnership(
            String analysisId
    ) {
        String userId = currentUserService.getUserId();

        analysisJobRepository
                .findByIdAndUserId(analysisId, userId)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ANALYSIS_NOT_FOUND
                        ));
    }
}