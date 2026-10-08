package com.apianalyzer.ingestion.api.webhook;

import com.apianalyzer.core.domain.entity.VcsConnection;
import com.apianalyzer.ingestion.application.service.vcs.GithubPrPublisherService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/github")
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookController {
    
    private final GithubPrPublisherService prPublisher;
    private final ObjectMapper objectMapper;
    
    @Value("${github.webhook.secret:defaultSecretForDev}")
    private String webhookSecret;
    
    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {
            
        // 1. Validate X-Hub-Signature-256 for security
        if (signature == null || !isValidSignature(rawPayload, signature)) {
            log.warn("Webhook rejected: Invalid or missing signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }
        
        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<Map<String, Object>>() {});
            
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
                    
                    log.info("Webhook received for PR #{}. Comparing {} to {}", prNumber, baseSha, headSha);
                    
                    // 3. Trigger AsyncAnalysisOrchestrator via messaging/events
                    // (Simulated output processing)
                    VcsConnection mockConnection = VcsConnection.builder().projectId(UUID.randomUUID()).build();
                    
                    // 4. Publish PR check/comment upon completion
                    prPublisher.publishPrComment(mockConnection, prNumber, 2, 5, "http://localhost:5173/analysis/mock-id");
                    
                    return ResponseEntity.ok("Analysis Triggered");
                }
            }
            return ResponseEntity.ok("Event ignored");
        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }
    
    private boolean isValidSignature(String payload, String signature) {
        try {
            String expectedSignature = "sha256=" + calculateHmac(payload, webhookSecret);
            return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
    
    private String calculateHmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
