package com.designmd.designapi.analysis;

import com.designmd.designapi.ai.DesignAnalysisResultRepository;
import com.designmd.designapi.ai.DesignAnalysisPublisher;
import com.designmd.designapi.analysis.request.CreateAnalysisRequest;
import com.designmd.designapi.analysis.response.AnalysisCreatedResponse;
import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.crawl.CrawlRequestPublisher;
import com.designmd.designapi.crawl.CrawledPageRepository;
import com.designmd.designapi.design.DesignSystemRepository;
import com.designmd.designapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AnalysisCommandService {
    private final AnalysisJobRepository repository;
    private final CurrentUserService currentUserService;
    private final CrawlRequestPublisher crawlRequestPublisher;
    private final CrawledPageRepository crawledPageRepository;
    private final DesignSystemRepository
            designSystemRepository;
    private final DesignAnalysisResultRepository
            designAnalysisResultRepository;
    private final DesignAnalysisPublisher designAnalysisPublisher;
    private final AnalysisUrlNormalizer urlNormalizer;

    @Value("${analysis.cache.ttl:PT168H}")
    private Duration cacheTtl;

    public AnalysisCreatedResponse create(
            CreateAnalysisRequest request
    ) {
        Instant now = Instant.now();
        String userId = currentUserService.getUserId();
        AnalysisUrlNormalizer.NormalizedAnalysis normalized =
                urlNormalizer.normalize(
                        request.websiteUrl(),
                        request.additionalPaths(),
                        request.includeScreenshot()
                );

        if (!request.forceRefresh()) {
            var cached = repository
                    .findFirstByUserIdAndCacheKeyAndStatusAndCompletedAtAfterOrderByCompletedAtDesc(
                            userId,
                            normalized.cacheKey(),
                            AnalysisStatus.COMPLETED,
                            now.minus(cacheTtl)
                    )
                    .filter(job ->
                            designAnalysisResultRepository
                                    .existsByAnalysisJobId(job.getId())
                    );

            if (cached.isPresent()) {
                AnalysisJob job = cached.get();
                return toCreatedResponse(job, true, job.getId());
            }
        }

        AnalysisJob job = AnalysisJob.builder()
                .userId(userId)
                .websiteUrl(request.websiteUrl())
                .normalizedUrl(normalized.normalizedUrl())
                .domain(normalized.domain())
                .cacheKey(normalized.cacheKey())
                .additionalPaths(
                        request.additionalPaths() == null
                                ? List.of()
                                : request.additionalPaths()
                )
                .includeScreenshot(request.includeScreenshot())
                .status(AnalysisStatus.PENDING)
                .progress(0)
                .createdAt(now)
                .updatedAt(now)
                .cacheHit(false)
                .build();

        AnalysisJob savedJob = repository.save(job);

        /*
         * Cập nhật CRAWLING trước khi publish để tránh trường hợp
         * worker xử lý quá nhanh và completed event quay lại khi
         * job vẫn còn PENDING.
         */
        savedJob.setStatus(AnalysisStatus.CRAWLING);
        savedJob.setProgress(20);
        savedJob.setUpdatedAt(Instant.now());

        savedJob = repository.save(savedJob);

        try {
            crawlRequestPublisher.publish(savedJob);
        } catch (AmqpException exception) {
            savedJob.setStatus(AnalysisStatus.FAILED);
            savedJob.setProgress(0);
            savedJob.setErrorCode("CRAWL_DISPATCH_FAILED");
            savedJob.setErrorMessage(exception.getMessage());
            savedJob.setUpdatedAt(Instant.now());
            repository.save(savedJob);
            throw exception;
        }

        return toCreatedResponse(savedJob, false, null);
    }

    public AnalysisCreatedResponse regenerateArtifact(String analysisId) {
        String userId = currentUserService.getUserId();
        AnalysisJob job = repository
                .findByIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ANALYSIS_NOT_FOUND));

        var snapshot = designSystemRepository
                .findByAnalysisJobId(analysisId)
                .orElseThrow(() -> new AppException(ErrorCode.ANALYSIS_NOT_COMPLETED));

        AnalysisStatus previousStatus = job.getStatus();
        job.setStatus(AnalysisStatus.GENERATING_MARKDOWN);
        job.setProgress(90);
        job.setUpdatedAt(Instant.now());
        repository.save(job);

        try {
            designAnalysisPublisher.publish(snapshot);
        } catch (AmqpException exception) {
            job.setStatus(previousStatus);
            job.setProgress(previousStatus == AnalysisStatus.COMPLETED ? 100 : job.getProgress());
            job.setUpdatedAt(Instant.now());
            repository.save(job);
            throw exception;
        }

        return toCreatedResponse(job, false, analysisId);
    }

    private AnalysisCreatedResponse toCreatedResponse(
            AnalysisJob job,
            boolean cacheHit,
            String sourceAnalysisId
    ) {
        return new AnalysisCreatedResponse(
                job.getId(),
                job.getWebsiteUrl(),
                job.getStatus(),
                job.getProgress(),
                cacheHit,
                sourceAnalysisId
        );
    }

    public void delete(String analysisId) {
        String userId = currentUserService.getUserId();

        AnalysisJob job = repository
                .findByIdAndUserId(analysisId, userId)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ANALYSIS_NOT_FOUND
                        ));

        if (job.getStatus() == AnalysisStatus.CRAWLING ||
                job.getStatus() == AnalysisStatus.ANALYZING) {
            throw new AppException(
                    ErrorCode.ANALYSIS_CANNOT_BE_DELETED
            );
        }

        designSystemRepository.deleteByAnalysisJobId(
                analysisId
        );

        crawledPageRepository.deleteAllByAnalysisJobId(
                analysisId
        );
        designAnalysisResultRepository.deleteByAnalysisJobId(
                analysisId
        );
        repository.delete(job);

    }
}
