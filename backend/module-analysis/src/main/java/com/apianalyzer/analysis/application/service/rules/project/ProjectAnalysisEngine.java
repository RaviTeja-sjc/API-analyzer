package com.apianalyzer.analysis.application.service.rules.project;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectAnalysisEngine {

    private final List<ProjectAnalysisRule> rules;

    public List<ProjectIssue> runAnalysis(ProjectAnalysisContext context) {
        List<ProjectIssue> allIssues = new ArrayList<>();
        
        for (ProjectAnalysisRule rule : rules) {
            try {
                List<ProjectIssue> issues = rule.execute(context);
                if (issues != null) {
                    allIssues.addAll(issues);
                }
            } catch (Exception e) {
                // In production, we'd log rule failure and continue.
            }
        }
        
        return allIssues;
    }
}
