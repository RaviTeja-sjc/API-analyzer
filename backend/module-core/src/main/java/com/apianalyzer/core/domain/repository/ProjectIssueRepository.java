package com.apianalyzer.core.domain.repository;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProjectIssueRepository extends JpaRepository<ProjectIssue, UUID> {
    List<ProjectIssue> findByProjectId(UUID projectId);
    List<ProjectIssue> findByAnalysisJobId(UUID analysisJobId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM migration_proposals WHERE project_issue_id IN (SELECT id FROM project_issues WHERE project_id = :projectId)", nativeQuery = true)
    void deleteMigrationProposalsByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProjectIssue pi WHERE pi.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
