package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.ApiSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface ApiSpecRepository extends JpaRepository<ApiSpec, UUID> {
    Optional<ApiSpec> findByProjectIdAndName(UUID projectId, String name);
}


