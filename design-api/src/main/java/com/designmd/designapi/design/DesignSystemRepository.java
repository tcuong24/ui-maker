package com.designmd.designapi.design;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DesignSystemRepository
        extends MongoRepository<DesignSystemSnapshot, String> {

    Optional<DesignSystemSnapshot> findByAnalysisJobId(
            String analysisJobId
    );

    void deleteByAnalysisJobId(String analysisJobId);
}