package com.apianalyzer.analysis.application.service.migration;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ApiConsumerImpact;
import com.apianalyzer.analysis.domain.model.migration.MigrationSuggestion;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class MigrationEngineService {
    
    public List<MigrationSuggestion> generateSuggestions(ApiChange change, List<ApiConsumerImpact> impacts) {
        List<MigrationSuggestion> suggestions = new ArrayList<>();
        
        for (ApiConsumerImpact impact : impacts) {
            MigrationSuggestion suggestion = MigrationSuggestion.builder()
                .affectedFile(impact.getFileName())
                .affectedSymbol(impact.getClassName() + "." + impact.getMethodName())
                .build();
                
            switch (change.getType()) {
                case PARAMETER_REMOVED:
                    suggestion.setOldUsage("Using parameter: " + extractName(change.getDescription()));
                    suggestion.setNewUsage("Remove parameter from request");
                    suggestion.setExplanation("The API endpoint no longer accepts this parameter. Remove it from your FeignClient or RestTemplate call.");
                    suggestion.setConfidence(0.95);
                    suggestion.setRequiresManualReview(false);
                    break;
                    
                case PARAMETER_CHANGED: // e.g. Optional to Required
                    suggestion.setOldUsage("Passing null or omitting parameter: " + extractName(change.getDescription()));
                    suggestion.setNewUsage("Provide a valid non-null value for the parameter.");
                    suggestion.setExplanation("The parameter has become REQUIRED. Ensure that your client always provides a value to avoid 400 Bad Request errors.");
                    suggestion.setConfidence(0.90);
                    suggestion.setRequiresManualReview(true); // Dev must decide what value to pass
                    break;
                    
                case ENDPOINT_REMOVED:
                    suggestion.setOldUsage("Calling " + change.getMethod() + " " + change.getPath());
                    suggestion.setNewUsage("Migrate to v2 or alternative endpoint.");
                    suggestion.setExplanation("This endpoint has been entirely removed from the OpenAPI specification.");
                    suggestion.setConfidence(1.0);
                    suggestion.setRequiresManualReview(true);
                    break;
                    
                case SCHEMA_PROPERTY_REMOVED:
                    String prop = extractName(change.getDescription());
                    suggestion.setOldUsage("Reading/Writing property: " + prop);
                    suggestion.setNewUsage("Remove mapping for " + prop + " in DTO");
                    suggestion.setExplanation("The JSON property '" + prop + "' was removed. Remove it from your Java DTOs to avoid unmapped property warnings.");
                    suggestion.setConfidence(0.80);
                    suggestion.setRequiresManualReview(false);
                    break;
                    
                case SCHEMA_TYPE_CHANGED:
                    suggestion.setOldUsage("Type: " + change.getOldValue());
                    suggestion.setNewUsage("Change Type to: " + change.getNewValue());
                    suggestion.setExplanation("The data type of a property changed. Update your DTO to match the new type to avoid Jackson Deserialization errors.");
                    suggestion.setConfidence(0.85);
                    suggestion.setRequiresManualReview(true);
                    break;
                    
                case SECURITY_CHANGED:
                    suggestion.setOldUsage(change.getOldValue());
                    suggestion.setNewUsage(change.getNewValue());
                    suggestion.setExplanation("Authentication requirements changed. Ensure your interceptors or security context pass the correct tokens.");
                    suggestion.setConfidence(1.0);
                    suggestion.setRequiresManualReview(true);
                    break;
                    
                default:
                    suggestion.setExplanation("Review the change description and verify compatibility.");
                    suggestion.setConfidence(0.4);
                    suggestion.setRequiresManualReview(true);
            }
            suggestions.add(suggestion);
        }
        return suggestions;
    }
    
    private String extractName(String description) {
        if (description == null) return "Unknown";
        String[] parts = description.split(":");
        return parts.length > 1 ? parts[1].trim() : parts[0];
    }
}

