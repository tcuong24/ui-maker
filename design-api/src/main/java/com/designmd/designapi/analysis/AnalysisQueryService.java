package com.designmd.designapi.analysis;

import com.designmd.designapi.analysis.response.AnalysisDetailResponse;
import com.designmd.designapi.analysis.response.AnalysisSummaryResponse;
import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.crawl.CrawledPageMapper;
import com.designmd.designapi.crawl.CrawledPageService;
import com.designmd.designapi.crawl.response.CrawledPageResponse;
import com.designmd.designapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisQueryService {
        private final AnalysisJobRepository repository;
        private final AnalysisMapper mapper;
        private final CurrentUserService currentUserService;
        private final CrawledPageService crawledPageService;
        private final CrawledPageMapper crawledPageMapper;

        public List<CrawledPageResponse> getPages(
                        String analysisId) {
                String userId = currentUserService.getUserId();

                AnalysisJob job = repository
                                .findByIdAndUserId(
                                                analysisId,
                                                userId)
                                .orElseThrow(() -> new AppException(
                                                ErrorCode.ANALYSIS_NOT_FOUND));

                if (job.getStatus() == AnalysisStatus.FAILED) {
                        throw new AppException(
                                        ErrorCode.ANALYSIS_CRAWL_FAILED);
                }

                if (!job.getStatus().hasCrawlResult()) {
                        throw new AppException(
                                        ErrorCode.ANALYSIS_NOT_COMPLETED);
                }

                return crawledPageMapper.toResponse(
                                crawledPageService.getByAnalysisId(
                                                analysisId));
        }

        public Page<AnalysisSummaryResponse> getMyAnalyses(
                        Pageable pageable) {
                String userId = currentUserService.getUserId();

                return repository
                                .findAllByUserId(userId, pageable)
                                .map(mapper::toSummaryResponse);
        }

        public AnalysisDetailResponse getById(String analysisId) {
                String userId = currentUserService.getUserId();

                AnalysisJob job = repository
                                .findByIdAndUserId(analysisId, userId)
                                .orElseThrow(() -> new AppException(
                                                ErrorCode.ANALYSIS_NOT_FOUND));

                return mapper.toDetailResponse(job);
        }
}
