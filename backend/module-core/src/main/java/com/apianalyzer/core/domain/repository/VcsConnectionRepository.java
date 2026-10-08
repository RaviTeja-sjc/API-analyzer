package com.apianalyzer.core.domain.repository;

import com.apianalyzer.core.domain.entity.VcsConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VcsConnectionRepository extends JpaRepository<VcsConnection, UUID> {
    Optional<VcsConnection> findByProjectId(UUID projectId);
}
