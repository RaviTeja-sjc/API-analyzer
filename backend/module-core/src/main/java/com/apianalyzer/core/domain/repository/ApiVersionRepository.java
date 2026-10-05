package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.ApiVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface ApiVersionRepository extends JpaRepository<ApiVersion, UUID> {
    Optional<ApiVersion> findByApiSpecIdAndVersionTag(UUID specId, String version);
}


