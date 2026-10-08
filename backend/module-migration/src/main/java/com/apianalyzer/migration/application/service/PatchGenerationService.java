package com.apianalyzer.migration.application.service;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.migration.application.service.rules.MigrationRule;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import com.apianalyzer.migration.domain.repository.MigrationProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatchGenerationService {

    private final List<MigrationRule> migrationRules;
    private final MigrationProposalRepository proposalRepository;

    public List<MigrationProposal> generateProposals(List<ProjectIssue> issues, RepositorySnapshot snapshot) {
        List<MigrationProposal> generated = new ArrayList<>();

        for (ProjectIssue issue : issues) {
            MigrationRule matchedRule = migrationRules.stream()
                    .filter(rule -> rule.supports(issue))
                    .findFirst()
                    .orElse(null);

            if (matchedRule != null) {
                MigrationProposal proposal = matchedRule.generateProposal(issue, snapshot);
                
                // Validate Proposal
                if (proposal.isAutoFixSupported() && proposal.getProposedContent() != null && !proposal.getProposedContent().equals(proposal.getOriginalContent())) {
                    String actualFileContent = snapshot.getFiles().stream()
                            .filter(f -> f.getPath().equals(proposal.getFilePath()))
                            .findFirst()
                            .map(RepositorySnapshot.RepositoryFile::getContent)
                            .orElse("");
                            
                    if (actualFileContent.contains(issue.getEvidence())) {
                        proposal.setValidationStatus(MigrationProposal.ValidationStatus.VALID);
                        
                        // Compute a simple hash of the file content for stale checking
                        String contentHash = Integer.toHexString(actualFileContent.hashCode());
                        proposal.setOriginalContentHash(contentHash);
                        
                        // Generate a safe diff
                        String safeFileContent = actualFileContent.replace(issue.getEvidence(), proposal.getOriginalContent());
                        String patchedFileContent = actualFileContent.replace(issue.getEvidence(), proposal.getProposedContent());
                        
                        proposal.setUnifiedDiff(generateSimpleDiff(proposal.getFilePath(), safeFileContent, patchedFileContent, issue.getStartLine()));
                        
                    } else {
                        proposal.setValidationStatus(MigrationProposal.ValidationStatus.INVALID);
                    }
                } else if (!proposal.isAutoFixSupported()) {
                    proposal.setValidationStatus(MigrationProposal.ValidationStatus.VALID); // Manual is always valid to show
                }

                generated.add(proposal);
            }
        }

        return proposalRepository.saveAll(generated);
    }
    
    public boolean isProposalStale(MigrationProposal proposal, RepositorySnapshot currentSnapshot) {
        if (proposal.getOriginalContentHash() == null) {
            return false;
        }
        String currentFileContent = currentSnapshot.getFiles().stream()
                .filter(f -> f.getPath().equals(proposal.getFilePath()))
                .findFirst()
                .map(RepositorySnapshot.RepositoryFile::getContent)
                .orElse("");
        
        String currentHash = Integer.toHexString(currentFileContent.hashCode());
        return !proposal.getOriginalContentHash().equals(currentHash);
    }
    
    private String generateSimpleDiff(String filePath, String safeFileContent, String patchedFileContent, Integer startLine) {
        // Simplified diff representation for demonstration/safety since we are doing simple replacements
        int lineNum = startLine != null ? startLine : 1;
        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(filePath).append("\n");
        diff.append("+++ b/").append(filePath).append("\n");
        diff.append("@@ -").append(lineNum).append(" +").append(lineNum).append(" @@\n");
        
        String[] originalLines = safeFileContent.split("\n");
        String[] newLines = patchedFileContent.split("\n");
        
        // Output context lines around the change
        int startIdx = Math.max(0, lineNum - 3);
        int endIdx = Math.min(originalLines.length, lineNum + 3);
        
        for (int i = startIdx; i < endIdx; i++) {
            if (i == lineNum - 1) {
                diff.append("-").append(originalLines[i]).append("\n");
                if (i < newLines.length) {
                    diff.append("+").append(newLines[i]).append("\n");
                }
            } else {
                diff.append(" ").append(originalLines[i]).append("\n");
            }
        }
        
        return diff.toString();
    }
}
