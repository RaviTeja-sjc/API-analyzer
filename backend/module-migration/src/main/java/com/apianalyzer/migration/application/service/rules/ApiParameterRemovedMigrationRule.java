package com.apianalyzer.migration.application.service.rules;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApiParameterRemovedMigrationRule implements MigrationRule {

    private static final String RULE_ID = "API-PARAMETER_REMOVED";

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
                .title("Manual Migration: API Parameter Removed")
                .description("The parameter identified in " + issue.getFilePath() + " was removed. Cannot safely modify client calls without explicit consumer mapping.")
                .filePath(issue.getFilePath())
                .startLine(issue.getStartLine())
                .endLine(issue.getEndLine())
                .originalContent(issue.getEvidence())
                .proposedContent(null)
                .unifiedDiff(null)
                .rationale("Removing API parameters requires manual review to ensure consumer code compiles and behaves correctly.")
                .validationStatus(MigrationProposal.ValidationStatus.PENDING_VALIDATION)
                .applyStatus(MigrationProposal.ApplyStatus.GENERATED)
                .autoFixSupported(false)
                .build();
    }
}
