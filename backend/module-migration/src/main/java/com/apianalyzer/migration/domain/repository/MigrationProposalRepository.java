package com.apianalyzer.migration.domain.repository;

import com.apianalyzer.migration.domain.entity.MigrationProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MigrationProposalRepository extends JpaRepository<MigrationProposal, UUID> {
    List<MigrationProposal> findByAnalysisJobId(UUID analysisJobId);
    List<MigrationProposal> findByProjectIssueId(UUID projectIssueId);
}
