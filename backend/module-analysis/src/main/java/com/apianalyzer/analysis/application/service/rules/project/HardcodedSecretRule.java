package com.apianalyzer.analysis.application.service.rules.project;

import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HardcodedSecretRule implements ProjectAnalysisRule {

    // Simple regex to catch hardcoded passwords like: password = "my-secret" or password="abc"
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(password|secret|api[_-]?key)\\s*=\\s*\"([^\"]+)\"");

    @Override
    public String getRuleId() {
        return "SEC-HARDCODED-SECRET";
    }

    @Override
    public String getCategory() {
        return "SECURITY";
    }

    @Override
    public String getDescription() {
        return "Detects hardcoded secrets in source files.";
    }

    @Override
    public List<ProjectIssue> execute(ProjectAnalysisContext context) {
        List<ProjectIssue> issues = new ArrayList<>();
        
        for (RepositorySnapshot.RepositoryFile file : context.getSnapshot().getFiles()) {
            // Only scan likely source code or config files
            if (isTextFile(file.getPath())) {
                String content = context.getRepositoryProvider().fetchFileContent(
                        context.getSnapshot().getUrl(), 
                        context.getSnapshot().getHeadCommitSha(), 
                        file.getPath()
                );
                
                if (content != null) {
                    scanContent(content, file.getPath(), context, issues);
                }
            }
        }
        
        return issues;
    }
    
    private void scanContent(String content, String filePath, ProjectAnalysisContext context, List<ProjectIssue> issues) {
        String[] lines = content.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = SECRET_PATTERN.matcher(line);
            if (matcher.find()) {
                String key = matcher.group(1);
                String fullMatch = matcher.group(0);
                String secretValue = matcher.group(2);
                
                // Redact the secret in the evidence
                String redactedEvidence = fullMatch.replace(secretValue, "[REDACTED]");
                
                ProjectIssue issue = ProjectIssue.builder()
                        .projectId(context.getProjectId())
                        .analysisJobId(context.getAnalysisJobId())
                        .category(getCategory())
                        .ruleId(getRuleId())
                        .severity("HIGH")
                        .confidence("0.94") // high confidence for explicit string assignments
                        .title("Hardcoded Secret Detected in " + filePath)
                        .problemDescription("A hardcoded secret (" + key + ") was found.")
                        .impact("HIGH")
                        .filePath(filePath)
                        .startLine(i + 1)
                        .endLine(i + 1)
                        .evidence(redactedEvidence)
                        .build();
                        
                issues.add(issue);
            }
        }
    }
    
    private boolean isTextFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".java") || lower.endsWith(".js") || lower.endsWith(".ts") 
               || lower.endsWith(".py") || lower.endsWith(".json") || lower.endsWith(".yml") 
               || lower.endsWith(".yaml") || lower.endsWith(".properties") || lower.endsWith(".xml");
    }
}
