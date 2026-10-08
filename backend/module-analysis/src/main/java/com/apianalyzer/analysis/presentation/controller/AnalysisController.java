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
            
            List<Map<String, Object>> changes = new ArrayList<>();
            if (latestJob != null && latestJob.getResultPayload() != null && !latestJob.getResultPayload().isEmpty()) {
                try {
                    Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        latestJob.getResultPayload(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){}
                    );
                    
                    if (payloadMap.containsKey("apiChanges")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> apiChanges = (List<Map<String, Object>>) payloadMap.get("apiChanges");
                        if (apiChanges != null) {
                            for (Map<String, Object> diffChange : apiChanges) {
                                Map<String, Object> change = new HashMap<>();
                                change.put("endpointMethod", diffChange.get("method"));
                                change.put("endpointPath", diffChange.get("path"));
                                change.put("changeType", diffChange.get("type"));
                                change.put("breakingStatus", diffChange.get("impact") != null ? diffChange.get("impact").toString() : "NON_BREAKING");
                                change.put("severityLevel", diffChange.get("severity"));
                                change.put("description", diffChange.get("description"));
                                change.put("oldValue", diffChange.get("oldValue"));
                                change.put("newValue", diffChange.get("newValue"));
                                
                                List<String> realImpactReasons = new ArrayList<>();
                                realImpactReasons.add("Detected via OpenAPI diff.");
                                change.put("impactReasons", realImpactReasons);
                                change.put("warnings", new ArrayList<>());
                                change.put("impactedConsumers", new ArrayList<>());
                                
                                changes.add(change);
                            }
                        }
                    }
                    
                    if (payloadMap.containsKey("dependencyGraph")) {
                        apiAnalysis.put("dependencyGraph", payloadMap.get("dependencyGraph"));
                    }
                } catch (Exception e) {}
            }
            
            long breakingChanges = changes.stream().filter(c -> "BREAKING".equals(c.get("breakingStatus"))).count();
            long potentiallyBreaking = changes.stream().filter(c -> "POTENTIALLY_BREAKING".equals(c.get("breakingStatus"))).count();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalChanges", changes.size());
            summary.put("breakingChanges", breakingChanges);
            summary.put("potentiallyBreakingChanges", potentiallyBreaking);
            summary.put("totalImpactedConsumers", 0);
            apiAnalysis.put("summary", summary);
            apiAnalysis.put("changes", changes);
            
            response.put("apiAnalysis", apiAnalysis);
            return ResponseEntity.ok(response);
        }
        
        // No GitHub URL or error fetching
        apiAnalysis.put("baseVersion", "N/A");
        apiAnalysis.put("headVersion", "N/A");
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalChanges", 0);
        summary.put("breakingChanges", 0);
        summary.put("potentiallyBreakingChanges", 0);
        summary.put("totalImpactedConsumers", 0);
        apiAnalysis.put("summary", summary);
        
        apiAnalysis.put("changes", new ArrayList<>());
        
        // Load Dependency Graph if available (from Repository analyzer)
        if (latestJob != null && latestJob.getResultPayload() != null && !latestJob.getResultPayload().isEmpty()) {
            try {
                Map<String, Object> graphData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    latestJob.getResultPayload(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){}
                );
                apiAnalysis.put("dependencyGraph", graphData);
            } catch (Exception e) {}
        }
        
        // Phase 5: Add Scores to Response
        if (latestJob != null && latestJob.getScoreBreakdown() != null && !latestJob.getScoreBreakdown().isEmpty()) {
            try {
                Map<String, Object> scoreData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    latestJob.getScoreBreakdown(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){}
                );
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
