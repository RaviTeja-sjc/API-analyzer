package com.apianalyzer.analysis.application.service;

import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.entity.Project;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import com.apianalyzer.core.domain.repository.ProjectRepository;
import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import com.apianalyzer.core.domain.event.AnalysisJobCompletedEvent;

@Service
@RequiredArgsConstructor
public class AsyncAnalysisOrchestrator {

    private final AnalysisJobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final RepositoryProvider repositoryProvider;
    private final ApiSpecDiscoveryService apiSpecDiscoveryService;
    private final OpenApiNormalizer openApiNormalizer;
    private final ApiDiffEngine apiDiffEngine;
    private final ObjectMapper objectMapper;
    private final ProjectIssueRepository projectIssueRepository;
    private final ApiChangeToProjectIssueMapper apiChangeToProjectIssueMapper;
    private final com.apianalyzer.analysis.application.service.rules.project.ProjectAnalysisEngine projectAnalysisEngine;
    private final com.apianalyzer.analysis.application.service.scoring.ScoringEngine scoringEngine;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public AnalysisJob submitJob(UUID projectId, String baseVer, String headVer, String idempotencyKey) {
        var existing = jobRepository.findByIdempotencyKeyAndStatusIn(idempotencyKey, List.of("PENDING", "RUNNING", "COMPLETED"));
        if (existing.isPresent()) {
            return existing.get();
        }
        
        AnalysisJob job = AnalysisJob.builder()
            .projectId(projectId)
            .status("PENDING")
            .progress(0)
            .idempotencyKey(idempotencyKey)
            .logs("Job submitted for analysis")
            .build();
        return jobRepository.save(job);
    }
    
    @Async
    public CompletableFuture<Void> processJobAsync(UUID jobId) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow();
        if ("CANCELLED".equals(job.getStatus())) return CompletableFuture.completedFuture(null);
        
        try {
            updateStatus(job, "RUNNING", 10, "Starting real analysis pipeline...");
            
            Project project = projectRepository.findById(job.getProjectId()).orElseThrow();
            if (project.getRepositoryUrl() == null || project.getRepositoryUrl().isEmpty()) {
                updateStatus(job, "FAILED", 100, "Project does not have a linked repository URL.");
                return CompletableFuture.completedFuture(null);
            }
            
            if (isCancelled(jobId)) return CompletableFuture.completedFuture(null);
            
            // Phase 1: Real Repository Acquisition & Inventory
            updateStatus(job, "RUNNING", 30, "Acquiring repository snapshot from " + project.getRepositoryUrl() + "...");
            
            RepositorySnapshot snapshot = repositoryProvider.fetchSnapshot(project.getRepositoryUrl(), "HEAD");
            
            if (snapshot.getFiles() == null || snapshot.getFiles().isEmpty()) {
                updateStatus(job, "FAILED", 100, "Repository is empty or inaccessible.");
                return CompletableFuture.completedFuture(null);
            }
            
            updateStatus(job, "RUNNING", 50, "Performing file discovery and language detection...");
            
            int totalFiles = snapshot.getFiles().size();
            int filesAnalyzed = 0;
            int filesSkipped = 0;
            int unsupportedFiles = 0;
            
            Map<String, Integer> languageCounts = new HashMap<>();
            
            for (RepositorySnapshot.RepositoryFile file : snapshot.getFiles()) {
                if (!"blob".equals(file.getType())) {
                    continue;
                }
                
                String path = file.getPath().toLowerCase();
                
                if (isIgnoredPath(path)) {
                    filesSkipped++;
                    continue;
                }
                
                String ext = getExtension(path);
                if (ext != null) {
                    languageCounts.put(ext, languageCounts.getOrDefault(ext, 0) + 1);
                    filesAnalyzed++;
                } else {
                    unsupportedFiles++;
                }
            }
            
            job.setTotalFilesDiscovered(totalFiles);
            job.setFilesAnalyzed(filesAnalyzed);
            job.setFilesSkipped(filesSkipped);
            job.setUnsupportedFiles(unsupportedFiles);
            
            String langStr = languageCounts.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(", "));
            job.setLanguages(langStr);
            
            jobRepository.save(job);
            
            if (isCancelled(jobId)) return CompletableFuture.completedFuture(null);
            
            updateStatus(job, "RUNNING", 80, "Inventory complete. " + totalFiles + " files discovered.");
            
            // Phase 2: API Analysis
            updateStatus(job, "RUNNING", 85, "Discovering API Specifications in HEAD and BASE (HEAD~1)...");
            
            RepositorySnapshot baseSnapshot = repositoryProvider.fetchSnapshot(project.getRepositoryUrl(), "HEAD~1"); // Using HEAD~1 as base
            
            java.util.Optional<ApiSpecDiscoveryService.DiscoveredSpec> baseSpec = apiSpecDiscoveryService.discoverOpenApiSpec(repositoryProvider, baseSnapshot);
            java.util.Optional<ApiSpecDiscoveryService.DiscoveredSpec> headSpec = apiSpecDiscoveryService.discoverOpenApiSpec(repositoryProvider, snapshot);
            String apiFilePath = headSpec.isPresent() ? headSpec.get().getFilePath() : null;
            
            List<ApiChange> apiChanges = new ArrayList<>();
            if (baseSpec.isPresent() && headSpec.isPresent()) {
                updateStatus(job, "RUNNING", 90, "Parsing and Normalizing OpenAPI specs...");
                NormalizedApiModel.Api baseApi = openApiNormalizer.normalize(baseSpec.get().getContent());
                NormalizedApiModel.Api headApi = openApiNormalizer.normalize(headSpec.get().getContent());
                
                updateStatus(job, "RUNNING", 95, "Running Semantic API Diff Engine...");
                apiChanges = apiDiffEngine.compare(baseApi, headApi);
            } else if (headSpec.isPresent() && !baseSpec.isPresent()) {
                updateStatus(job, "RUNNING", 95, "Base spec not found. Treating all endpoints as added.");
                NormalizedApiModel.Api baseApi = new NormalizedApiModel.Api(); // empty base
                NormalizedApiModel.Api headApi = openApiNormalizer.normalize(headSpec.get().getContent());
                apiChanges = apiDiffEngine.compare(baseApi, headApi);
            } else {
                updateStatus(job, "RUNNING", 95, "API analysis unsupported (no OpenAPI spec found in HEAD).");
            }
            
            // Delete old issues for this job if any (shouldn't be, but good for isolation)
            projectIssueRepository.findByAnalysisJobId(jobId).forEach(projectIssueRepository::delete);
            
            // Persist finding issues
            if (!apiChanges.isEmpty()) {
                updateStatus(job, "RUNNING", 97, "Persisting " + apiChanges.size() + " API findings...");
                List<ProjectIssue> issues = apiChanges.stream()
                        .map(change -> apiChangeToProjectIssueMapper.map(change, project.getId(), jobId, apiFilePath))
                        .collect(Collectors.toList());
                projectIssueRepository.saveAll(issues);
            }
            
            // Phase 4: Project Static Analysis
            updateStatus(job, "RUNNING", 98, "Running project-wide static analysis rules...");
            com.apianalyzer.analysis.application.service.rules.project.ProjectAnalysisContext context = 
                com.apianalyzer.analysis.application.service.rules.project.ProjectAnalysisContext.builder()
                .projectId(project.getId())
                .analysisJobId(jobId)
                .project(project)
                .snapshot(snapshot)
                .repositoryProvider(repositoryProvider)
                .build();
            
            List<ProjectIssue> projectIssues = projectAnalysisEngine.runAnalysis(context);
            if (!projectIssues.isEmpty()) {
                projectIssueRepository.saveAll(projectIssues);
            }
            
            // Phase 5: Real Deterministic Scoring
            updateStatus(job, "RUNNING", 99, "Calculating final scores...");
            boolean apiAnalyzed = headSpec.isPresent();
            scoringEngine.calculateAndPersistScores(jobId, apiAnalyzed);
            
            // Refresh job from DB to get the new scores for result payload
            job = jobRepository.findById(jobId).orElse(job);
            
            Map<String, Object> resultPayloadMap = new HashMap<>();
            
            // Try to retain the dependency graph if it already exists in the payload (from other orchestrators)
            if (job.getResultPayload() != null && !job.getResultPayload().isEmpty()) {
                try {
                    Map<String, Object> existing = objectMapper.readValue(job.getResultPayload(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                    resultPayloadMap.putAll(existing);
                } catch (Exception e) {}
            }
            
            resultPayloadMap.put("apiChanges", apiChanges);
            
            job.setResultPayload(objectMapper.writeValueAsString(resultPayloadMap));
            jobRepository.save(job);
            updateStatus(job, "COMPLETED", 100, "Analysis completed successfully.");
            
            eventPublisher.publishEvent(AnalysisJobCompletedEvent.builder()
                .analysisJobId(jobId)
                .projectId(project.getId())
                .repositoryUrl(project.getRepositoryUrl())
                .headCommitSha(snapshot.getHeadCommitSha())
                .build());
            
        } catch (Exception e) {
            updateStatus(job, "FAILED", job.getProgress(), "Fatal error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    private boolean isIgnoredPath(String path) {
        return path.contains("node_modules/") || path.contains("target/") || 
               path.contains("build/") || path.contains(".git/") || path.contains("dist/");
    }
    
    private String getExtension(String path) {
        if (path.endsWith(".java")) return "Java";
        if (path.endsWith(".py")) return "Python";
        if (path.endsWith(".ts") || path.endsWith(".tsx")) return "TypeScript";
        if (path.endsWith(".js") || path.endsWith(".jsx")) return "JavaScript";
        if (path.endsWith(".sql")) return "SQL";
        if (path.endsWith(".yml") || path.endsWith(".yaml")) return "YAML";
        if (path.endsWith(".json")) return "JSON";
        if (path.endsWith(".xml")) return "XML";
        return null;
    }
    
    public void cancelJob(UUID jobId) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow();
        if ("PENDING".equals(job.getStatus()) || "RUNNING".equals(job.getStatus())) {
            updateStatus(job, "CANCELLED", job.getProgress(), "Job was manually cancelled.");
        }
    }
    
    private void updateStatus(AnalysisJob job, String status, int progress, String log) {
        job.setStatus(status);
        job.setProgress(progress);
        job.appendLog(log);
        jobRepository.save(job);
    }
    
    private boolean isCancelled(UUID jobId) {
        return jobRepository.findById(jobId).map(j -> "CANCELLED".equals(j.getStatus())).orElse(true);
    }
}
