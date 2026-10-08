package com.apianalyzer.analysis.application.service.rules.project;

import com.apianalyzer.analysis.application.service.ast.JavaAstParserService;
import com.apianalyzer.analysis.domain.model.ast.AstModels;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InappropriateLayerCouplingRule implements ProjectAnalysisRule {

    private final JavaAstParserService astParserService;

    @Override
    public String getRuleId() {
        return "ARCH-LAYER-COUPLING";
    }

    @Override
    public String getCategory() {
        return "ARCHITECTURE";
    }

    @Override
    public String getDescription() {
        return "Detects direct coupling from Controller layer to Repository layer.";
    }

    @Override
    public List<ProjectIssue> execute(ProjectAnalysisContext context) {
        List<ProjectIssue> issues = new ArrayList<>();

        for (RepositorySnapshot.RepositoryFile file : context.getSnapshot().getFiles()) {
            if (file.getPath().toLowerCase().endsWith(".java")) {
                String content = context.getRepositoryProvider().fetchFileContent(
                        context.getSnapshot().getUrl(), 
                        context.getSnapshot().getHeadCommitSha(), 
                        file.getPath()
                );
                
                if (content != null) {
                    try {
                        List<AstModels.ClassInfo> classes = astParserService.parseSourceCode(content, file.getPath());
                        for (AstModels.ClassInfo classInfo : classes) {
                            if (classInfo.isController()) {
                                // Check if this controller directly references a Repository
                                // Since we don't have perfect semantic resolution, we look for field types ending with Repository
                                for (String ref : classInfo.getTypeReferences()) {
                                    if (ref.endsWith("Repository") && !ref.contains("Service")) {
                                        ProjectIssue issue = ProjectIssue.builder()
                                                .projectId(context.getProjectId())
                                                .analysisJobId(context.getAnalysisJobId())
                                                .category(getCategory())
                                                .ruleId(getRuleId())
                                                .severity("MEDIUM")
                                                .confidence("0.8")
                                                .title("Direct Repository Access in Controller")
                                                .problemDescription("Controller " + classInfo.getClassName() + " directly accesses " + ref + " bypassing the Service layer.")
                                                .impact("MEDIUM")
                                                .filePath(file.getPath())
                                                .startLine(null)
                                                .endLine(null)
                                                .evidence("Class " + classInfo.getClassName() + " (Controller) has a reference to " + ref)
                                                .build();
                                        issues.add(issue);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore unparseable java files
                    }
                }
            }
        }
        
        return issues;
    }
}
