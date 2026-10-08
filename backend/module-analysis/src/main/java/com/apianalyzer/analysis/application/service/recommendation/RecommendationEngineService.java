package com.apianalyzer.analysis.application.service.recommendation;

import com.apianalyzer.analysis.application.dto.RecommendationDto;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationEngineService {

    public List<RecommendationDto> generateRecommendations(List<ProjectIssue> issues) {
        List<RecommendationDto> recommendations = new ArrayList<>();
        
        for (ProjectIssue issue : issues) {
            RecommendationDto dto = createRecommendationForIssue(issue);
            if (dto != null) {
                recommendations.add(dto);
            }
        }
        
        return recommendations;
    }
    
    private RecommendationDto createRecommendationForIssue(ProjectIssue issue) {
        if (issue.getRuleId() == null) {
            return null;
        }
        
        String title = "";
        String explanation = "";
        String remediation = "";
        
        switch (issue.getRuleId()) {
            case "HARDCODED_SECRET":
                title = "Secure Hardcoded Credentials";
                explanation = "A hardcoded secret was found in the source code. Storing secrets in version control compromises system security.";
                remediation = "Move the secret to environment configuration or a managed secret store (e.g. AWS Secrets Manager, Vault) and rotate the exposed credential immediately.";
                break;
            case "INAPPROPRIATE_LAYER_COUPLING":
                title = "Decouple Controller from Repository";
                explanation = "The controller directly references a repository layer. This violates separation of concerns and makes testing difficult.";
                remediation = "Introduce or use a service layer component between the controller and repository. Inject the service into the controller.";
                break;
            case "ENDPOINT_REMOVED":
            case "API-ENDPOINT_REMOVED":
                title = "API Endpoint Deprecation";
                explanation = "An existing API endpoint was removed, which is a breaking change for current consumers.";
                remediation = "Review downstream consumers and introduce a compatibility strategy such as deprecation warnings or versioning before completely removing the endpoint.";
                break;
            case "PARAMETER_REMOVED":
            case "API-PARAMETER_REMOVED":
                title = "API Parameter Deprecation";
                explanation = "A required API parameter was removed or modified in a breaking way.";
                remediation = "Review API consumers and consider a backward-compatible migration path, such as supporting both old and new parameters temporarily.";
                break;
            case "PARAMETER_TYPE_CHANGED":
            case "API-PARAMETER_TYPE_CHANGED":
                title = "API Parameter Type Compatibility";
                explanation = "An API parameter type was changed, which can break existing consumers.";
                remediation = "Introduce a new version of the endpoint or accept a loosely typed parameter that can parse both the old and new types during the transition period.";
                break;
            default:
                // We only generate recommendations for known issues. No generic filler.
                return null;
        }
        
        return RecommendationDto.builder()
                .projectIssueId(issue.getId())
                .ruleId(issue.getRuleId())
                .category(issue.getCategory())
                .title(title)
                .explanation(explanation)
                .remediation(remediation)
                .priority(issue.getSeverity())
                .filePath(issue.getFilePath())
                .startLine(issue.getStartLine())
                .endLine(issue.getEndLine())
                .evidence(issue.getEvidence())
                .build();
    }
}
