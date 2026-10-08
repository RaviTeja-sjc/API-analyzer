package com.apianalyzer.migration.application.service.rules;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FallbackMigrationRule implements MigrationRule {

    @Override
    public boolean supports(ProjectIssue issue) {
        return true;
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
                .title("Manual Migration Required")
                .description("Automatic remediation is not currently supported for " + issue.getRuleId() + ".")
                .filePath(issue.getFilePath())
                .startLine(issue.getStartLine())
                .endLine(issue.getEndLine())
                .originalContent(issue.getEvidence())
                .proposedContent(null)
                .unifiedDiff(null)
                .rationale("Review the issue and apply changes manually.")
                .validationStatus(MigrationProposal.ValidationStatus.PENDING_VALIDATION)
                .applyStatus(MigrationProposal.ApplyStatus.GENERATED)
                .autoFixSupported(false)
                .build();
    }
}
