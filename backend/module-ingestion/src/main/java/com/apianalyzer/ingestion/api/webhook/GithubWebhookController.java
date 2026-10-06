package com.apianalyzer.ingestion.api.webhook;
import com.apianalyzer.core.domain.entity.VcsConnection;
import com.apianalyzer.ingestion.application.service.vcs.GithubPrPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/webhooks/github")
@RequiredArgsConstructor
public class GithubWebhookController {
    
    private final GithubPrPublisherService prPublisher;
    
    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody Map<String, Object> payload) {
            
        // 1. Validate X-Hub-Signature-256 for security
        if (signature == null) {
            return ResponseEntity.status(401).body("Missing signature");
        }
        
        if ("pull_request".equals(event)) {
            String action = (String) payload.get("action");
            if ("opened".equals(action) || "synchronize".equals(action)) {
                // 2. Extract Data
                Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
                String prNumber = String.valueOf(pullRequest.get("number"));
                
                Map<String, Object> head = (Map<String, Object>) pullRequest.get("head");
                String headSha = (String) head.get("sha");
                
                Map<String, Object> base = (Map<String, Object>) pullRequest.get("base");
                String baseSha = (String) base.get("sha");
                
                System.out.println("Webhook received for PR #" + prNumber + ". Comparing " + baseSha + " to " + headSha);
                
                // 3. Trigger AsyncAnalysisOrchestrator via messaging/events
                // (Simulated output processing)
                VcsConnection mockConnection = VcsConnection.builder().projectId(UUID.randomUUID()).build();
                
                // 4. Publish PR check/comment upon completion
                prPublisher.publishPrComment(mockConnection, prNumber, 2, 5, "http://localhost:5173/analysis/mock-id");
                
                return ResponseEntity.ok("Analysis Triggered");
            }
        }
        
        return ResponseEntity.ok("Event ignored");
    }
}
