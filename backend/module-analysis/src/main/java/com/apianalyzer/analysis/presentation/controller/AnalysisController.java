package com.apianalyzer.analysis.presentation.controller;

import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.entity.Project;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import com.apianalyzer.core.domain.repository.ProjectRepository;
import com.apianalyzer.analysis.application.service.AsyncAnalysisOrchestrator;
import com.apianalyzer.analysis.application.service.RepositoryAnalysisOrchestrator;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.apianalyzer.analysis.application.service.recommendation.RecommendationEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisJobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final AsyncAnalysisOrchestrator analysisOrchestrator;
    private final RepositoryAnalysisOrchestrator repositoryAnalysisOrchestrator;

    private final ProjectIssueRepository projectIssueRepository;
    private final RecommendationEngineService recommendationEngineService;

    @PostMapping("/{projectId}/repository-analysis")
    public ResponseEntity<Map<String, Object>> triggerRepositoryAnalysis(@PathVariable UUID projectId) {
        AnalysisJob job = analysisOrchestrator.submitJob(
                projectId, "N/A", "HEAD", "REPO-" + UUID.randomUUID().toString()
        );
        repositoryAnalysisOrchestrator.processRepositoryAnalysisAsync(job.getId(), projectId);
        return ResponseEntity.ok(Map.of("status", "TRIGGERED", "jobId", job.getId()));
    }

    @PostMapping("/{projectId}/trigger")
    public ResponseEntity<Map<String, Object>> triggerAnalysis(@PathVariable UUID projectId) {
        AnalysisJob job = analysisOrchestrator.submitJob(
                projectId, "v1.2.0", "HEAD", UUID.randomUUID().toString()
        );
        analysisOrchestrator.processJobAsync(job.getId());
        return ResponseEntity.ok(Map.of("status", "TRIGGERED", "jobId", job.getId()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Map<String, Object>> getAnalysisReport(@PathVariable UUID projectId) {
        List<AnalysisJob> jobs = jobRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        Project project = projectRepository.findById(projectId).orElse(null);
        
        AnalysisJob latestJob = null;
        if (!jobs.isEmpty()) {
            latestJob = jobs.get(0);
        }

        Map<String, Object> response = new HashMap<>();

        if (latestJob != null && ("PENDING".equals(latestJob.getStatus()) || "RUNNING".equals(latestJob.getStatus()))) {
            response.put("status", latestJob.getStatus());
            response.put("progress", latestJob.getProgress());
            response.put("logs", latestJob.getLogs());
            return ResponseEntity.ok(response);
        }

        if (latestJob != null && "FAILED".equals(latestJob.getStatus())) {
            response.put("status", "FAILED");
            response.put("progress", latestJob.getProgress());
            response.put("logs", latestJob.getLogs());
            response.put("error", latestJob.getLogs());
            return ResponseEntity.ok(response);
        }
        
        response.put("status", "COMPLETED");
        
        List<ProjectIssue> issues = projectIssueRepository.findByProjectId(projectId);
        
        Map<String, Object> projectAnalysis = new HashMap<>();
        if (!issues.isEmpty()) {
            projectAnalysis.put("summary", buildProjectSummary(issues));
            projectAnalysis.put("issues", issues);
        } else {
            projectAnalysis.put("summary", Map.of("total", 0));
            projectAnalysis.put("issues", new ArrayList<>());
        }
        
        // Phase 7: Real Evidence-Based Recommendations
        response.put("recommendations", recommendationEngineService.generateRecommendations(issues));
        
        response.put("projectAnalysis", projectAnalysis);
        
        Map<String, Object> apiAnalysis = new HashMap<>();
        
        if (project != null && project.getRepositoryUrl() != null) {
            apiAnalysis.put("baseVersion", "HEAD~1");
            apiAnalysis.put("headVersion", "HEAD");
        } else {
            apiAnalysis.put("baseVersion", "N/A");
            apiAnalysis.put("headVersion", "N/A");
        }

        List<Map<String, Object>> changes = new ArrayList<>();
        String apiSpecStatus = "NO_SPEC_DISCOVERED";
        String apiSpecPath = null;
        Object apiExaminedFiles = new ArrayList<>();
        Object apiMatchedFiles = new ArrayList<>();

        if (latestJob != null && latestJob.getResultPayload() != null && !latestJob.getResultPayload().isEmpty()) {
            try {
                Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    latestJob.getResultPayload(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){}
                );
                
                if (payloadMap.containsKey("apiSpecStatus")) {
                    apiSpecStatus = String.valueOf(payloadMap.get("apiSpecStatus"));
                }
                if (payloadMap.containsKey("apiSpecPath")) {
                    apiSpecPath = (String) payloadMap.get("apiSpecPath");
                }
                if (payloadMap.containsKey("apiExaminedFiles")) {
                    apiExaminedFiles = payloadMap.get("apiExaminedFiles");
                }
                if (payloadMap.containsKey("apiMatchedFiles")) {
                    apiMatchedFiles = payloadMap.get("apiMatchedFiles");
                }

                if (payloadMap.containsKey("apiChanges")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> apiChanges = (List<Map<String, Object>>) payloadMap.get("apiChanges");
                    if (apiChanges != null) {
                        for (Map<String, Object> diffChange : apiChanges) {
                            Map<String, Object> change = new HashMap<>();
                            change.put("endpointMethod", diffChange.get("method"));
                            change.put("endpointPath", diffChange.get("path"));
                            change.put("changeType", diffChange.get("type"));
                            
                            String rawImpact = diffChange.get("impact") != null ? diffChange.get("impact").toString() : "";
                            String rawSeverity = diffChange.get("severity") != null ? diffChange.get("severity").toString() : "";
                            
                            String breakingStatus = "NON_BREAKING";
                            if ("BREAKING".equalsIgnoreCase(rawImpact) || "BREAKING".equalsIgnoreCase(rawSeverity)) {
                                breakingStatus = "BREAKING";
                            } else if ("POTENTIALLY_BREAKING".equalsIgnoreCase(rawImpact) || "POTENTIALLY_BREAKING".equalsIgnoreCase(rawSeverity)) {
                                breakingStatus = "POTENTIALLY_BREAKING";
                            }
                            
                            change.put("breakingStatus", breakingStatus);
                            change.put("severityLevel", !rawSeverity.isEmpty() ? rawSeverity : breakingStatus);
                            
                            int impactScore = 10;
                            if ("BREAKING".equals(breakingStatus)) impactScore = 95;
                            else if ("POTENTIALLY_BREAKING".equals(breakingStatus)) impactScore = 50;
                            change.put("impactScore", impactScore);

                            change.put("description", diffChange.get("description"));
                            change.put("oldValue", diffChange.get("oldValue"));
                            change.put("newValue", diffChange.get("newValue"));
                            
                            List<String> realImpactReasons = new ArrayList<>();
                            realImpactReasons.add("Detected via OpenAPI structural diff.");
                            change.put("impactReasons", realImpactReasons);
                            change.put("warnings", new ArrayList<>());
                            
                            Object consumers = diffChange.get("impactedConsumers");
                            if (consumers instanceof List) {
                                change.put("impactedConsumers", consumers);
                            } else {
                                change.put("impactedConsumers", new ArrayList<>());
                            }
                            
                            changes.add(change);
                        }
                    }
                }
                
                if (payloadMap.containsKey("dependencyGraph")) {
                    apiAnalysis.put("dependencyGraph", payloadMap.get("dependencyGraph"));
                } else if (payloadMap.containsKey("nodes") && payloadMap.containsKey("edges")) {
                    apiAnalysis.put("dependencyGraph", payloadMap);
                }
            } catch (Exception e) {}
        }
        
        long breakingChanges = changes.stream().filter(c -> "BREAKING".equals(c.get("breakingStatus"))).count();
        long potentiallyBreaking = changes.stream().filter(c -> "POTENTIALLY_BREAKING".equals(c.get("breakingStatus"))).count();
        
        java.util.Set<String> uniqueConsumers = new java.util.HashSet<>();
        for (Map<String, Object> c : changes) {
            Object consumersObj = c.get("impactedConsumers");
            if (consumersObj instanceof List<?>) {
                for (Object consumer : (List<?>) consumersObj) {
                    if (consumer instanceof Map<?, ?> m) {
                        Object name = m.get("consumerName");
                        if (name != null) uniqueConsumers.add(name.toString());
                    }
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalChanges", changes.size());
        summary.put("breakingChanges", breakingChanges);
        summary.put("potentiallyBreakingChanges", potentiallyBreaking);
        summary.put("totalImpactedConsumers", uniqueConsumers.size());
        apiAnalysis.put("summary", summary);
        apiAnalysis.put("changes", changes);
        apiAnalysis.put("apiSpecStatus", apiSpecStatus);
        apiAnalysis.put("apiSpecPath", apiSpecPath);
        apiAnalysis.put("apiExaminedFiles", apiExaminedFiles);
        apiAnalysis.put("apiMatchedFiles", apiMatchedFiles);
        
        // Phase 5: Add Scores to Response
        if (latestJob != null && latestJob.getScoreBreakdown() != null && !latestJob.getScoreBreakdown().isEmpty()) {
            try {
                Map<String, Object> scoreData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    latestJob.getScoreBreakdown(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){}
                );
                if (!scoreData.containsKey("securityScore") && scoreData.containsKey("security")) {
                    Object sec = scoreData.get("security");
                    if (sec instanceof Map && ((Map<?, ?>) sec).containsKey("finalScore")) {
                        scoreData.put("securityScore", ((Map<?, ?>) sec).get("finalScore"));
                    }
                }
                if (!scoreData.containsKey("apiHealthScore") && scoreData.containsKey("apiHealth")) {
                    Object health = scoreData.get("apiHealth");
                    if (health instanceof Map && ((Map<?, ?>) health).containsKey("finalScore")) {
                        scoreData.put("apiHealthScore", ((Map<?, ?>) health).get("finalScore"));
                    }
                }
                response.put("scoring", scoreData);
            } catch (Exception e) {}
        }
        
        // Phase 1/8: Add Coverage Info
        if (latestJob != null) {
            Map<String, Object> coverage = new HashMap<>();
            coverage.put("totalFiles", latestJob.getTotalFilesDiscovered() != null ? latestJob.getTotalFilesDiscovered() : 0);
            coverage.put("analyzedFiles", latestJob.getFilesAnalyzed() != null ? latestJob.getFilesAnalyzed() : 0);
            coverage.put("skippedFiles", latestJob.getFilesSkipped() != null ? latestJob.getFilesSkipped() : 0);
            coverage.put("unsupportedFiles", latestJob.getUnsupportedFiles() != null ? latestJob.getUnsupportedFiles() : 0);
            if (latestJob.getLanguages() != null && !latestJob.getLanguages().isEmpty()) {
                try {
                    Map<String, Integer> langs = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        latestJob.getLanguages(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>(){}
                    );
                    coverage.put("detectedLanguages", langs);
                } catch (Exception e) {}
            } else {
                coverage.put("detectedLanguages", new HashMap<>());
            }
            response.put("coverage", coverage);
        }
        
        response.put("apiAnalysis", apiAnalysis);
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> buildProjectSummary(List<ProjectIssue> issues) {
        long critical = issues.stream().filter(i -> "CRITICAL".equals(i.getSeverity())).count();
        long high = issues.stream().filter(i -> "HIGH".equals(i.getSeverity())).count();
        long medium = issues.stream().filter(i -> "MEDIUM".equals(i.getSeverity())).count();
        long low = issues.stream().filter(i -> "LOW".equals(i.getSeverity())).count();
        long info = issues.stream().filter(i -> "INFO".equals(i.getSeverity())).count();
        
        String risk = critical > 0 ? "CRITICAL" : (high > 0 ? "HIGH" : (medium > 0 ? "MEDIUM" : "LOW"));
        
        return Map.of(
            "total", issues.size(),
            "critical", critical,
            "high", high,
            "medium", medium,
            "low", low,
            "info", info,
            "overallRisk", risk
        );
    }
}
