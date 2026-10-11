package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.application.service.ast.JavaAstParserService;
import com.apianalyzer.analysis.application.service.graph.DependencyGraphBuilderService;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel;
import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodCallInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodInfo;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ImpactedConsumer;
import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.entity.Project;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.event.AnalysisJobCompletedEvent;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.apianalyzer.core.domain.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final JavaAstParserService javaAstParserService;
    private final DependencyGraphBuilderService graphBuilderService;

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
            updateStatus(job, "RUNNING", 5, "Starting real repository analysis pipeline...");

            Project project = projectRepository.findById(job.getProjectId()).orElseThrow();
            if (project.getRepositoryUrl() == null || project.getRepositoryUrl().isEmpty()) {
                updateStatus(job, "FAILED", 100, "Project does not have a linked repository URL.");
                return CompletableFuture.completedFuture(null);
            }

            if (isCancelled(jobId)) return CompletableFuture.completedFuture(null);

            // Phase 1: Real Repository Acquisition & Inventory
            updateStatus(job, "RUNNING", 15, "Acquiring repository file tree from " + project.getRepositoryUrl() + "...");

            RepositorySnapshot snapshot = repositoryProvider.fetchSnapshot(project.getRepositoryUrl(), "HEAD");

            if (snapshot.getFiles() == null || snapshot.getFiles().isEmpty()) {
                updateStatus(job, "FAILED", 100, "Repository is empty or inaccessible.");
                return CompletableFuture.completedFuture(null);
            }

            updateStatus(job, "RUNNING", 30, "Performing file discovery and language detection...");

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

            updateStatus(job, "RUNNING", 45, "Inventory complete: " + totalFiles + " files discovered across " + languageCounts.size() + " languages.");

            // Phase 2: API Specification Discovery & Semantic Diffing
            updateStatus(job, "RUNNING", 55, "Discovering OpenAPI specifications in HEAD and BASE (HEAD~1)...");

            RepositorySnapshot baseSnapshot = null;
            try {
                baseSnapshot = repositoryProvider.fetchSnapshot(project.getRepositoryUrl(), "HEAD~1");
            } catch (Exception e) {
                log.warn("Could not fetch BASE (HEAD~1) snapshot: {}. Single-revision analysis mode.", e.getMessage());
            }

            ApiSpecDiscoveryService.DiscoveryReport headReport = apiSpecDiscoveryService.discoverWithReport(repositoryProvider, snapshot);
            ApiSpecDiscoveryService.DiscoveryReport baseReport = (baseSnapshot != null)
                    ? apiSpecDiscoveryService.discoverWithReport(repositoryProvider, baseSnapshot)
                    : new ApiSpecDiscoveryService.DiscoveryReport(Optional.empty(), Collections.emptyList(), Collections.emptyList(), List.of("No BASE revision available"));

            Optional<ApiSpecDiscoveryService.DiscoveredSpec> headSpec = headReport.getPrimarySpec();
            Optional<ApiSpecDiscoveryService.DiscoveredSpec> baseSpec = baseReport.getPrimarySpec();

            String apiFilePath = headSpec.map(ApiSpecDiscoveryService.DiscoveredSpec::getFilePath).orElse(null);
            List<ApiChange> apiChanges = new ArrayList<>();
            String apiSpecStatus;

            if (headSpec.isPresent() && baseSpec.isPresent()) {
                updateStatus(job, "RUNNING", 65, "Parsing & normalizing specifications from HEAD (" + headSpec.get().getFilePath() + ") and BASE...");
                NormalizedApiModel.Api baseApi = openApiNormalizer.normalize(baseSpec.get().getContent());
                NormalizedApiModel.Api headApi = openApiNormalizer.normalize(headSpec.get().getContent());

                updateStatus(job, "RUNNING", 75, "Executing Semantic API Diff Engine across revisions...");
                apiChanges = apiDiffEngine.compare(baseApi, headApi);
                apiSpecStatus = "DISCOVERED";
            } else if (headSpec.isPresent()) {
                updateStatus(job, "RUNNING", 70, "Base spec not found in HEAD~1. Performing initial specification inventory...");
                NormalizedApiModel.Api baseApi = new NormalizedApiModel.Api();
                NormalizedApiModel.Api headApi = openApiNormalizer.normalize(headSpec.get().getContent());
                apiChanges = apiDiffEngine.compare(baseApi, headApi);
                apiSpecStatus = "DISCOVERED";
            } else {
                updateStatus(job, "RUNNING", 70, "No OpenAPI specification discovered in repository.");
                apiSpecStatus = "NO_SPEC_DISCOVERED";
            }

            // Phase 3: Java AST Parsing & Source Impact Analysis
            updateStatus(job, "RUNNING", 80, "Parsing Java source files and extracting AST mappings...");
            List<ClassInfo> allClasses = new ArrayList<>();
            List<Map<String, Object>> allFilesPayload = new ArrayList<>();

            // Parse Java files from repository
            for (RepositorySnapshot.RepositoryFile file : snapshot.getFiles()) {
                if (!"blob".equals(file.getType())) continue;
                String path = file.getPath();
                if (isIgnoredPath(path.toLowerCase())) continue;

                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("path", path);
                allFilesPayload.add(fileMap);

                if (path.endsWith(".java")) {
                    try {
                        String content = repositoryProvider.fetchFileContent(project.getRepositoryUrl(), snapshot.getHeadCommitSha(), path);
                        if (content != null && !content.isBlank()) {
                            file.setContent(content); // cache content for migration rule validation
                            List<ClassInfo> fileClasses = javaAstParserService.parseSourceCode(content, path);
                            allClasses.addAll(fileClasses);
                        }
                    } catch (Exception ex) {
                        log.debug("Skipped parsing Java file {}: {}", path, ex.getMessage());
                    }
                }
            }

            // Phase 4: Link Breaking API Changes to AST Source Components
            updateStatus(job, "RUNNING", 88, "Tracing API breaking changes to internal Java Controllers & Services...");
            linkApiChangesToSourceComponents(apiChanges, allClasses);

            // Phase 5: Dependency Graph Construction
            updateStatus(job, "RUNNING", 92, "Constructing interactive blast-radius dependency graph...");
            String graphJson = graphBuilderService.buildImpactGraph(allClasses, allFilesPayload, apiChanges);
            Map<String, Object> graphMap = new HashMap<>();
            try {
                graphMap = objectMapper.readValue(graphJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                graphMap.put("nodes", Collections.emptyList());
                graphMap.put("edges", Collections.emptyList());
            }

            // Delete old proposals and issues for this project to ensure clean, idempotent state
            try {
                projectIssueRepository.deleteMigrationProposalsByProjectId(project.getId());
                projectIssueRepository.deleteByProjectId(project.getId());
            } catch (Exception ex) {
                log.warn("Could not bulk clean old issues/proposals for project {}: {}", project.getId(), ex.getMessage());
            }

            // Persist API Finding Issues
            if (!apiChanges.isEmpty()) {
                updateStatus(job, "RUNNING", 94, "Persisting " + apiChanges.size() + " API findings...");
                List<ProjectIssue> issues = apiChanges.stream()
                        .map(change -> apiChangeToProjectIssueMapper.map(change, project.getId(), jobId, apiFilePath))
                        .collect(Collectors.toList());
                projectIssueRepository.saveAll(issues);
            }

            // Phase 6: Project Static Analysis (Security, Architecture, Code Quality)
            updateStatus(job, "RUNNING", 96, "Running project-wide static analysis rules...");
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

            // Phase 7: Deterministic Scoring
            updateStatus(job, "RUNNING", 98, "Calculating API health and security scores...");
            boolean apiAnalyzed = headSpec.isPresent();
            scoringEngine.calculateAndPersistScores(jobId, apiAnalyzed);

            // Refresh job from DB
            job = jobRepository.findById(jobId).orElse(job);

            // Result Payload
            Map<String, Object> resultPayloadMap = new HashMap<>();
            resultPayloadMap.put("apiChanges", apiChanges);
            resultPayloadMap.put("dependencyGraph", graphMap);
            resultPayloadMap.put("apiSpecStatus", apiSpecStatus);
            resultPayloadMap.put("apiSpecPath", apiFilePath);
            resultPayloadMap.put("apiExaminedFiles", headReport.getExaminedFiles());
            resultPayloadMap.put("apiMatchedFiles", headReport.getMatchedFiles());

            job.setResultPayload(objectMapper.writeValueAsString(resultPayloadMap));
            jobRepository.save(job);

            updateStatus(job, "COMPLETED", 100, "Analysis completed successfully. " + apiChanges.size() + " API changes detected.");

            // Phase 8: Publish Completed Event for Automated Migration Proposals
            eventPublisher.publishEvent(AnalysisJobCompletedEvent.builder()
                    .analysisJobId(jobId)
                    .projectId(project.getId())
                    .repositoryUrl(project.getRepositoryUrl())
                    .headCommitSha(snapshot.getHeadCommitSha())
                    .build());

        } catch (Exception e) {
            log.error("Fatal error during analysis job {}: {}", jobId, e.getMessage(), e);
            updateStatus(job, "FAILED", job.getProgress(), "Fatal error during analysis: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private void linkApiChangesToSourceComponents(List<ApiChange> apiChanges, List<ClassInfo> allClasses) {
        if (apiChanges == null || allClasses == null || allClasses.isEmpty()) return;

        Map<String, ClassInfo> classByName = allClasses.stream()
                .collect(Collectors.toMap(ClassInfo::getClassName, c -> c, (c1, c2) -> c1));

        for (ApiChange change : apiChanges) {
            List<ImpactedConsumer> consumers = new ArrayList<>();
            String changeMethod = change.getMethod() != null ? change.getMethod().toUpperCase() : "";
            String changePath = normalizePath(change.getPath());

            for (ClassInfo controller : allClasses) {
                if (!controller.isController()) continue;

                for (MethodInfo method : controller.getMethods()) {
                    if (method.getFullEndpointPath() == null || method.getFullEndpointPath().isEmpty()) continue;

                    String methodPath = normalizePath(method.getFullEndpointPath());
                    boolean methodMatches = changeMethod.isEmpty() || method.getHttpMethod() == null || changeMethod.equalsIgnoreCase(method.getHttpMethod());

                    if (methodMatches && pathsMatch(changePath, methodPath)) {
                        consumers.add(ImpactedConsumer.builder()
                                .name(controller.getClassName() + "." + method.getName())
                                .type("CONTROLLER")
                                .filePath(controller.getFilePath())
                                .lineNumber(method.getStartLine())
                                .impactType("DIRECT_MATCH")
                                .evidence("@" + (method.getHttpMethod() != null ? method.getHttpMethod() : "Request") + "Mapping(\"" + method.getFullEndpointPath() + "\")")
                                .build());

                        // Trace downstream services invoked by this method
                        for (MethodCallInfo call : method.getMethodCalls()) {
                            String targetScope = call.getScope();
                            if (targetScope != null && classByName.containsKey(targetScope)) {
                                ClassInfo downstream = classByName.get(targetScope);
                                consumers.add(ImpactedConsumer.builder()
                                        .name(downstream.getClassName() + "." + call.getName())
                                        .type(downstream.isService() ? "SERVICE" : (downstream.isRepository() ? "REPOSITORY" : "DEPENDENCY"))
                                        .filePath(downstream.getFilePath())
                                        .lineNumber(method.getStartLine())
                                        .impactType("DOWNSTREAM_DEPENDENCY")
                                        .evidence("Invoked from " + controller.getClassName() + "." + method.getName() + " -> " + targetScope + "." + call.getName())
                                        .build());
                            }
                        }
                    }
                }
            }

            if (consumers.isEmpty()) {
                consumers.add(ImpactedConsumer.builder()
                        .name("Unresolved Java Binding")
                        .type("ENDPOINT")
                        .filePath(change.getPath())
                        .lineNumber(null)
                        .impactType("UNVERIFIED")
                        .evidence("No physical Spring @Controller mapping matched endpoint " + change.getMethod() + " " + change.getPath())
                        .build());
            }

            change.setImpactedConsumers(consumers);
        }
    }

    private String normalizePath(String path) {
        if (path == null) return "/";
        String p = path.trim().toLowerCase();
        if (!p.startsWith("/")) p = "/" + p;
        if (p.endsWith("/") && p.length() > 1) p = p.substring(0, p.length() - 1);
        p = p.replaceAll("\\{[^}]+\\}", "*");
        return p;
    }

    private boolean pathsMatch(String p1, String p2) {
        if (p1.equals(p2)) return true;
        String[] s1 = p1.split("/");
        String[] s2 = p2.split("/");
        if (s1.length != s2.length) return false;
        for (int i = 0; i < s1.length; i++) {
            if (!s1[i].equals(s2[i]) && !s1[i].equals("*") && !s2[i].equals("*")) {
                return false;
            }
        }
        return true;
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

    private void updateStatus(AnalysisJob job, String status, int progress, String logMsg) {
        job.setStatus(status);
        job.setProgress(progress);
        job.appendLog(logMsg);
        jobRepository.save(job);
    }

    private boolean isCancelled(UUID jobId) {
        return jobRepository.findById(jobId).map(j -> "CANCELLED".equals(j.getStatus())).orElse(true);
    }
}
