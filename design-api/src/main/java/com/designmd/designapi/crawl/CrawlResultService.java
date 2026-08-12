package com.designmd.designapi.crawl;

import com.designmd.designapi.ai.DesignAnalysisPublisher;
import com.designmd.designapi.analysis.AnalysisJob;
import com.designmd.designapi.analysis.AnalysisJobRepository;
import com.designmd.designapi.analysis.AnalysisStatus;
import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.design.DesignAggregationService;
import com.designmd.designapi.design.DesignSystemSnapshot;
import com.designmd.designapi.messaging.event.CrawlCompletedEvent;
import com.designmd.designapi.messaging.event.CrawlFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlResultService {

    private final AnalysisJobRepository repository;
    private final CrawledPageService crawledPageService;
    private final DesignAnalysisPublisher designAnalysisPublisher;
    private final DesignAggregationService
            designAggregationService;

    public void handleCompleted(
        CrawlCompletedEvent event
) {
    AnalysisJob job = findAnalysis(
            event.analysisJobId()
    );

    if (job.getStatus()
            == AnalysisStatus.CRAWL_COMPLETED) {
        log.info(
                "Analysis {} was already completed",
                job.getId()
        );
        return;
    }

    if (job.getStatus()
            != AnalysisStatus.CRAWLING) {
        log.warn(
                "Ignoring event {} because analysis "
                        + "{} has status {}",
                event.eventId(),
                job.getId(),
                job.getStatus()
        );
        return;
    }

    /*
     * Phải lưu pages thành công trước khi cập nhật status.
     * Nếu lưu lỗi, consumer sẽ throw và job không bị đánh
     * dấu CRAWL_COMPLETED sai.
     */
    crawledPageService.savePages(
            job.getId(),
            event.pages()
    );
        DesignSystemSnapshot snapshot =
                designAggregationService.rebuild(
                        job.getId()
                );

        job.setStatus(AnalysisStatus.ANALYZING);
        job.setProgress(60);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        job.setUpdatedAt(Instant.now());

        repository.save(job);

        designAnalysisPublisher.publish(snapshot);


    int pageCount = event.pages() == null
            ? 0
            : event.pages().size();

    log.info(
            "Analysis {} crawl completed "
                    + "and saved {} page(s)",
            job.getId(),
            pageCount
    );
}

    public void handleFailed(
            CrawlFailedEvent event
    ) {
        AnalysisJob job = findAnalysis(
                event.analysisJobId()
        );

        if (job.getStatus()
                == AnalysisStatus.FAILED) {
            log.info(
                    "Analysis {} was already marked failed",
                    job.getId()
            );
            return;
        }

        job.setStatus(AnalysisStatus.FAILED);
        job.setProgress(0);
        job.setErrorCode(event.errorCode());
        job.setErrorMessage(event.errorMessage());
        job.setUpdatedAt(Instant.now());

        repository.save(job);

        log.warn(
                "Analysis {} crawl failed: {}",
                job.getId(),
                event.errorMessage()
        );
    }

    private AnalysisJob findAnalysis(
            String analysisId
    ) {
        return repository
                .findById(analysisId)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ANALYSIS_NOT_FOUND
                        ));
    }
}