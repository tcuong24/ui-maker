package com.designmd.designapi.crawl;

import com.designmd.designapi.messaging.event.CrawledPagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawledPageService {

    private final CrawledPageRepository repository;

    public void savePages(
            String analysisJobId,
            List<CrawledPagePayload> payloads
    ) {
        if (payloads == null || payloads.isEmpty()) {
            return;
        }

        for (CrawledPagePayload payload : payloads) {
            savePage(analysisJobId, payload);
        }
    }

    private void savePage(
            String analysisJobId,
            CrawledPagePayload payload
    ) {
        Instant now = Instant.now();

        CrawledPage page = repository
                .findByAnalysisJobIdAndPageUrl(
                        analysisJobId,
                        payload.url()
                )
                .orElseGet(() ->
                        CrawledPage.builder()
                                .analysisJobId(analysisJobId)
                                .pageUrl(payload.url())
                                .createdAt(now)
                                .build()
                );

        page.setFinalUrl(payload.finalUrl());
        page.setTitle(payload.title());
        page.setDurationMs(payload.durationMs());
        page.setCssVariables(
                payload.cssVariables()
        );

        page.setColors(
                payload.colors()
        );

        page.setTypography(
                payload.typography()
        );

        page.setSpacing(
                payload.spacing()
        );

        page.setRadii(
                payload.radii()
        );

        page.setShadows(
                payload.shadows()
        );

        page.setUpdatedAt(now);
        repository.save(page);
    }

    public List<CrawledPage> getByAnalysisId(
            String analysisJobId
    ) {
        return repository
                .findAllByAnalysisJobIdOrderByCreatedAtAsc(
                        analysisJobId
                );
    }
}
