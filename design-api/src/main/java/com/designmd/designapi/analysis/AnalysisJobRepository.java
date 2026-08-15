package com.designmd.designapi.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface AnalysisJobRepository extends MongoRepository<AnalysisJob,String> {
    Page<AnalysisJob> findAllByUserId(
            String userId,
            Pageable pageable
    );

    Optional<AnalysisJob> findByIdAndUserId(
            String id,
            String userId
    );

    boolean existsByIdAndUserId(
            String id,
            String userId
    );

    void deleteByIdAndUserId(
            String id,
            String userId
    );

    Optional<AnalysisJob> findFirstByUserIdAndCacheKeyAndStatusAndCompletedAtAfterOrderByCompletedAtDesc(
            String userId,
            String cacheKey,
            AnalysisStatus status,
            Instant completedAfter
    );
}
