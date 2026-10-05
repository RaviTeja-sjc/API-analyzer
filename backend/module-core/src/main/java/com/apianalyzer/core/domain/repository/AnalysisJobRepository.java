package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
    Optional<AnalysisJob> findByIdempotencyKeyAndStatusIn(String idempotencyKey, java.util.List<String> statuses);
}
