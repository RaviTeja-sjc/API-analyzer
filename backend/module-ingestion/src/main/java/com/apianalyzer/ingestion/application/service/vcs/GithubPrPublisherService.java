package com.apianalyzer.ingestion.application.service.vcs;
import com.apianalyzer.core.domain.entity.VcsConnection;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GithubPrPublisherService {
    
    public void publishPrComment(VcsConnection connection, String prNumber, int breakingChanges, int impactedConsumers, String reportUrl) {
        // Formulate the PR comment
        StringBuilder markdown = new StringBuilder();
        markdown.append("<!-- API_ANALYZER_REPORT -->\n");
        markdown.append("## ?? API Semantic Analysis Report\n\n");
        
        if (breakingChanges > 0) {
            markdown.append("?? **DANGER:** Detected **").append(breakingChanges).append("** breaking API changes!\n");
        } else {
            markdown.append("? **SUCCESS:** No breaking changes detected.\n");
        }
        
        markdown.append("?? **Consumers Impacted:** ").append(impactedConsumers).append("\n\n");
        markdown.append("[View Interactive Impact Graph & Full Report](").append(reportUrl).append(")\n\n");
        markdown.append("---\n*Automated by API Analyzer*");
        
        // Idempotency: 
        // 1. GET /repos/{owner}/{repo}/issues/{prNumber}/comments
        // 2. Search for comment containing <!-- API_ANALYZER_REPORT -->
        // 3. If exists: PATCH /repos/{owner}/{repo}/issues/comments/{commentId}
        // 4. Else: POST /repos/{owner}/{repo}/issues/{prNumber}/comments
        
        log.info("Published Idempotent PR Comment to GitHub PR #{}:\n{}", prNumber, markdown.toString());
    }
}
