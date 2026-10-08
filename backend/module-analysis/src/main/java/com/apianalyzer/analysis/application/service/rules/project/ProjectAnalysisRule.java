package com.apianalyzer.analysis.application.service.rules.project;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import java.util.List;

public interface ProjectAnalysisRule {
    /**
     * @return the unique ID of the rule, e.g., SEC-HARDCODED-SECRET
     */
    String getRuleId();
    
    /**
     * @return the category of the rule, e.g., SECURITY, ARCHITECTURE, CODE_QUALITY
     */
    String getCategory();
    
    /**
     * @return a short description of what this rule detects
     */
    String getDescription();
    
    /**
     * Executes the rule against the project context.
     * @param context Context containing repository snapshot and provider for file fetching
     * @return A list of ProjectIssue entities representing findings. 
     */
    List<ProjectIssue> execute(ProjectAnalysisContext context);
}
