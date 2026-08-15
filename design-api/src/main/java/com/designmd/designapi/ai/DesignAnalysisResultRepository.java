package com.designmd.designapi.ai;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DesignAnalysisResultRepository
        extends MongoRepository<DesignAnalysisResult, String> {

    Optional<DesignAnalysisResult> findByAnalysisJobId(
            String analysisJobId
    );

    boolean existsByAnalysisJobId(String analysisJobId);

    void deleteByAnalysisJobId(String analysisJobId);
}
