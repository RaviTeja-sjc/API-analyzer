package com.apianalyzer.analysis.application.service;

import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.entity.Project;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import com.apianalyzer.core.domain.repository.ProjectRepository;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.apianalyzer.analysis.application.service.ast.JavaAstParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodCallInfo;

@Service
@RequiredArgsConstructor
public class RepositoryAnalysisOrchestrator {
    
    private final AnalysisJobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final ProjectIssueRepository projectIssueRepository;
    private final GithubIntegrationService githubIntegrationService;
    private final JavaAstParserService javaAstParserService;
    private final com.apianalyzer.analysis.application.service.graph.DependencyGraphBuilderService graphBuilderService;
    
    @Async
    public CompletableFuture<Void> processRepositoryAnalysisAsync(UUID jobId, UUID projectId) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();
        
        try {
            updateStatus(job, "RUNNING", 10, "Starting entire repository analysis...");
            
            if (project.getRepositoryUrl() == null || project.getRepositoryUrl().isEmpty()) {
                updateStatus(job, "FAILED", 100, "Project does not have a linked repository URL.");
                return CompletableFuture.completedFuture(null);
            }
            
            updateStatus(job, "RUNNING", 20, "Fetching repository file tree from GitHub...");
            List<Map<String, Object>> tree = githubIntegrationService.getRepositoryTree(project.getRepositoryUrl(), "HEAD");
            
            if (tree.isEmpty()) {
                updateStatus(job, "FAILED", 100, "Repository is empty or inaccessible.");
                return CompletableFuture.completedFuture(null);
            }
            
            int totalFiles = tree.size();
            int processed = 0;
            
            // Delete old issues for this project
            List<ProjectIssue> oldIssues = projectIssueRepository.findByProjectId(projectId);
            projectIssueRepository.deleteAll(oldIssues);
            
            Pattern secretPattern = Pattern.compile("(?i)(password|secret|token|api_key|apikey)\\s*=\\s*['\"]([^'\"]+)['\"]");
            
            List<ClassInfo> allClasses = new ArrayList<>();
            List<Map<String, Object>> allFiles = new ArrayList<>();
            
            for (Map<String, Object> node : tree) {
                processed++;
                if (processed % 10 == 0) {
                    updateStatus(job, "RUNNING", 20 + (int)(70.0 * processed / totalFiles), "Analyzing files... (" + processed + "/" + totalFiles + ")");
                }
                
                String type = (String) node.get("type");
                String path = (String) node.get("path");
                
                if (!"blob".equals(type) || isIgnoredPath(path)) continue;
                
                allFiles.add(node);
                
                // We only do deep analysis for Java files right now, but we check secrets everywhere.
                if (path.endsWith(".java") || path.endsWith(".properties") || path.endsWith(".yml") || path.endsWith(".yaml") || path.endsWith(".py") || path.endsWith(".ipynb")) {
                    
                    // Basic GitHub parsing to get owner/repo
                    String url = project.getRepositoryUrl();
                    java.util.regex.Matcher matcher = Pattern.compile("github\\.com/([^/]+)/([^/]+)").matcher(url);
                    if (matcher.find()) {
                        String owner = matcher.group(1);
                        String repo = matcher.group(2).replace(".git", "");
                        
                        String content = githubIntegrationService.getFileContent(owner, repo, "HEAD", path);
                        if (content == null) continue;
                        
                        // 1. Security Analysis (Secrets)
                        java.util.regex.Matcher secretMatcher = secretPattern.matcher(content);
                        if (secretMatcher.find()) {
                            saveIssue(projectId, jobId, "SECURITY", "HIGH", "HIGH", 
                                    "Hardcoded secret detected",
                                    "Found what appears to be a hardcoded credential or token.",
                                    "Hardcoded secrets can lead to unauthorized access and data breaches if the repository is compromised.",
                                    "Move this secret to an environment variable or secure vault.",
                                    path, null, secretMatcher.group(0));
                        }
                        
                        // 2. Java AST Analysis
                        if (path.endsWith(".java")) {
                            List<ClassInfo> fileClasses = analyzeJavaFile(content, path, projectId, jobId);
                            allClasses.addAll(fileClasses);
                        }
                    }
                }
            }
            
            // Generate Dependency Graph
            String graphJson = graphBuilderService.buildGraph(allClasses, allFiles);
            job.setResultPayload(graphJson);
            
            updateStatus(job, "COMPLETED", 100, "Repository analysis completed successfully.");
            
        } catch (Exception e) {
            updateStatus(job, "FAILED", job.getProgress(), "Fatal error during repository analysis: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private List<ClassInfo> analyzeJavaFile(String content, String path, UUID projectId, UUID jobId) {
        List<ClassInfo> classes = new ArrayList<>();
        try {
            classes = javaAstParserService.parseSourceCode(content, path);
            for (var cInfo : classes) {
                // Code Quality: Huge classes
                if (cInfo.getMethods().size() > 20) {
                    saveIssue(projectId, jobId, "CODE_QUALITY", "MEDIUM", "HIGH",
                            "Large Class Detected (" + cInfo.getClassName() + ")",
                            "This class has " + cInfo.getMethods().size() + " methods, indicating poor separation of concerns.",
                            "Large classes are harder to maintain, test, and understand.",
                            "Split this class into smaller, more focused components following the Single Responsibility Principle.",
                            path, null, "public class " + cInfo.getClassName() + " { ... }");
                }
                
                // Architecture violations
                if (cInfo.isController() && cInfo.getTypeReferences().contains("EntityManager")) {
                    saveIssue(projectId, jobId, "ARCHITECTURE", "HIGH", "HIGH",
                            "Controller / Data Layer Coupling",
                            "A REST Controller is directly injecting an EntityManager or Database reference.",
                            "Violates layering principles and makes the application harder to refactor and test.",
                            "Move data access logic to a Repository or Service layer.",
                            path, null, "@RestController public class " + cInfo.getClassName() + " { ... EntityManager ... }");
                }
                
                for (var mInfo : cInfo.getMethods()) {
                    // Code Quality: Huge methods
                    if (mInfo.getMethodCalls().size() > 15) {
                        saveIssue(projectId, jobId, "CODE_QUALITY", "LOW", "MEDIUM",
                                "High Method Complexity in " + mInfo.getName(),
                                "This method makes " + mInfo.getMethodCalls().size() + " sub-calls.",
                                "Increases cognitive load and chance of bugs.",
                                "Extract complex logic into smaller, well-named helper methods.",
                                path, null, mInfo.getName() + "(...) { ... }");
                    }
                    
                    // Performance: Loops with DB queries (naive check)
                    boolean hasLoop = content.contains("for (") || content.contains("while (");
                    if (hasLoop && mInfo.getMethodCalls().stream().anyMatch(call -> call.getName().startsWith("findBy") || call.getName().startsWith("save"))) {
                        saveIssue(projectId, jobId, "PERFORMANCE", "HIGH", "LOW", // Low confidence because naive string match
                                "Potential N+1 Query in " + mInfo.getName(),
                                "A database-like method call was detected in a method that contains a loop.",
                                "Can lead to severe performance degradation as the data set grows.",
                                "Use batch fetching or a single IN clause query instead of looping.",
                                path, null, "Loop detected with DB call in: " + mInfo.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parse errors for single files
        }
        return classes;
    }
    
    
    
    private void saveIssue(UUID projectId, UUID jobId, String category, String severity, String confidence, 
                           String title, String problem, String impact, String recommendation, String path, String lines, String evidence) {
        ProjectIssue issue = ProjectIssue.builder()
                .projectId(projectId)
                .analysisJobId(jobId)
                .category(category)
                .severity(severity)
                .confidence(confidence)
                .title(title)
                .problemDescription(problem)
                .impact(impact)
                .recommendation(recommendation)
                .filePath(path)
                .startLine(null)
                .endLine(null)
                .ruleId("PROJECT-" + category.toUpperCase())
                .evidence(evidence)
                .build();
        projectIssueRepository.save(issue);
    }
    
    private boolean isIgnoredPath(String path) {
        return path.contains("node_modules/") || path.contains("target/") || 
               path.contains("build/") || path.contains(".git/") || path.contains("dist/");
    }
    
    private void updateStatus(AnalysisJob job, String status, int progress, String log) {
        job.setStatus(status);
        job.setProgress(progress);
        job.appendLog(log);
        jobRepository.save(job);
    }
}
