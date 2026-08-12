package com.designmd.designapi.crawl;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CrawledPageRepository
        extends MongoRepository<CrawledPage, String> {

    List<CrawledPage> findAllByAnalysisJobIdOrderByCreatedAtAsc(
            String analysisJobId
    );

    Optional<CrawledPage> findByAnalysisJobIdAndPageUrl(
            String analysisJobId,
            String pageUrl
    );

    void deleteAllByAnalysisJobId(
            String analysisJobId
    );
}