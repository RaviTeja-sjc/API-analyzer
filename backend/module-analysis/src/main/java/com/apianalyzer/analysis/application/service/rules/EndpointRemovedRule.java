package com.apianalyzer.analysis.application.service.rules;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class EndpointRemovedRule implements DiffRule {
    @Override public String getName() { return "Endpoint Removed Rule"; }
    @Override
    public void evaluate(DiffContext context) {
        Map<String, Endpoint> headEndpoints = context.getHead().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e));
        
        for (Endpoint baseEp : context.getBase().getEndpoints()) {
            String key = baseEp.getMethod() + " " + baseEp.getPath();
            if (!headEndpoints.containsKey(key)) {
                context.addChange(ApiChange.builder().type(ChangeType.ENDPOINT_REMOVED).severity(ChangeSeverity.BREAKING)
                    .description("Endpoint removed: " + key).path(baseEp.getPath()).method(baseEp.getMethod())
                    .oldValue("Present").newValue("Removed").build());
            }
        }
    }
}
