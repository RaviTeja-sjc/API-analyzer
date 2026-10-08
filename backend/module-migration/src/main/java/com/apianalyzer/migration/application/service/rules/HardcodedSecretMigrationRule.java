package com.apianalyzer.migration.application.service.rules;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HardcodedSecretMigrationRule implements MigrationRule {

    private static final String RULE_ID = "HARDCODED_SECRET";
    private static final Pattern SECRET_PATTERN = Pattern.compile("([^=]+)\\s*=\\s*['\"]([^'\"]+)['\"]");

    @Override
    public boolean supports(ProjectIssue issue) {
        return RULE_ID.equals(issue.getRuleId());
    }

    @Override
    public MigrationProposal generateProposal(ProjectIssue issue, RepositorySnapshot snapshot) {
        String originalContent = issue.getEvidence();
        String proposedContent = originalContent;
        String rationale = "Extract hardcoded secret to environment variable.";
        boolean autoFix = false;
        
        if (originalContent != null && (issue.getFilePath().endsWith(".java") || issue.getFilePath().endsWith(".py"))) {
            Matcher m = SECRET_PATTERN.matcher(originalContent);
            if (m.find()) {
                String varName = m.group(1).trim();
                String secretValue = m.group(2);
                
                String envVarName = varName.toUpperCase().replaceAll("[^A-Z0-9]", "_");
                if (envVarName.isEmpty()) envVarName = "SECRET_VALUE";
                
                if (issue.getFilePath().endsWith(".java")) {
                    proposedContent = varName + " = System.getenv(\"" + envVarName + "\");";
                } else if (issue.getFilePath().endsWith(".py")) {
                    proposedContent = varName + " = os.getenv(\"" + envVarName + "\")";
                }
                
                originalContent = originalContent.replace(secretValue, "[REDACTED]");
                autoFix = true;
            }
        }

        String diff = "";

        return MigrationProposal.builder()
                .id(UUID.randomUUID())
                .analysisJobId(issue.getAnalysisJobId())
                .projectIssueId(issue.getId())
                .ruleId(issue.getRuleId())
                .category(issue.getCategory())
                .severity(issue.getSeverity())
                .title(autoFix ? "Extract hardcoded secret" : "Manual Secret Extraction Required")
                .description(autoFix ? "Automatically extract hardcoded secret into environment variables." : "Cannot safely auto-fix. Please move secret to secure configuration.")
                .filePath(issue.getFilePath())
                .startLine(issue.getStartLine())
                .endLine(issue.getEndLine())
                .originalContent(originalContent)
                .proposedContent(proposedContent)
                .unifiedDiff(diff)
                .rationale(rationale)
                .validationStatus(MigrationProposal.ValidationStatus.PENDING_VALIDATION)
                .applyStatus(MigrationProposal.ApplyStatus.GENERATED)
                .autoFixSupported(autoFix)
                .build();
    }
}
