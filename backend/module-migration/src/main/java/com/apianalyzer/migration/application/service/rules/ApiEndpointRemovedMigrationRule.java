package com.apianalyzer.migration.application.service.rules;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApiEndpointRemovedMigrationRule implements MigrationRule {

    private static final String RULE_ID = "API-ENDPOINT_REMOVED";

    @Override
    public boolean supports(ProjectIssue issue) {
        return RULE_ID.equals(issue.getRuleId());
    }

    @Override
    public MigrationProposal generateProposal(ProjectIssue issue, RepositorySnapshot snapshot) {
        return MigrationProposal.builder()
                .id(UUID.randomUUID())
                .analysisJobId(issue.getAnalysisJobId())
                .projectIssueId(issue.getId())
                .ruleId(issue.getRuleId())
                .category(issue.getCategory())
                .severity(issue.getSeverity())
                .title("Manual Migration: API Endpoint Removed")
                .description("The endpoint " + issue.getFilePath() + " was removed. Safe auto-fix is not possible without knowing upstream clients.")
                .filePath(issue.getFilePath())
                .startLine(issue.getStartLine())
                .endLine(issue.getEndLine())
                .originalContent(issue.getEvidence())
                .proposedContent(null)
                .unifiedDiff(null)
                .rationale("Removing API endpoints requires manual coordination with consumers.")
                .validationStatus(MigrationProposal.ValidationStatus.PENDING_VALIDATION)
                .applyStatus(MigrationProposal.ApplyStatus.GENERATED)
                .autoFixSupported(false)
                .build();
    }
}
