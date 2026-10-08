package com.apianalyzer.migration.application.service.rules;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;

public interface MigrationRule {
    boolean supports(ProjectIssue issue);
    MigrationProposal generateProposal(ProjectIssue issue, RepositorySnapshot snapshot);
}
