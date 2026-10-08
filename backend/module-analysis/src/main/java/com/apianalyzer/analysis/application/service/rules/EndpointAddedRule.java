package com.apianalyzer.analysis.application.service.rules;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EndpointAddedRule implements DiffRule {
    @Override 
    public String getName() { return "Endpoint Added Rule"; }
    
    @Override
    public void evaluate(DiffContext context) {
        Map<String, Endpoint> baseEndpoints = context.getBase().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e));
        
        for (Endpoint headEp : context.getHead().getEndpoints()) {
            String key = headEp.getMethod() + " " + headEp.getPath();
            if (!baseEndpoints.containsKey(key)) {
                context.addChange(ApiChange.builder()
                    .type(ChangeType.ENDPOINT_ADDED)
                    .severity(ChangeSeverity.NON_BREAKING)
                    .description("New endpoint added: " + key)
                    .path(headEp.getPath())
                    .method(headEp.getMethod())
                    .oldValue("Missing")
                    .newValue("Present")
                    .build());
            }
        }
    }
}
