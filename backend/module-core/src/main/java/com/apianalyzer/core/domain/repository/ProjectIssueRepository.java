package com.apianalyzer.core.domain.repository;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectIssueRepository extends JpaRepository<ProjectIssue, UUID> {
    List<ProjectIssue> findByProjectId(UUID projectId);
    List<ProjectIssue> findByAnalysisJobId(UUID analysisJobId);
}
