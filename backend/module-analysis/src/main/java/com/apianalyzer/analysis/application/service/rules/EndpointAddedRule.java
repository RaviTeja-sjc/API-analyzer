package com.apianalyzer.analysis.application.service.rules;

import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EndpointAddedRule implements DiffRule {
    @Override
    public String getName() { return "Endpoint Added Rule"; }

    @Override
    public void evaluate(DiffContext context) {
        if (context.getBase() == null || context.getHead() == null) return;

        Map<String, Endpoint> baseEndpoints = context.getBase().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (e1, e2) -> e1));

        for (Endpoint headEp : context.getHead().getEndpoints()) {
            String key = headEp.getMethod() + " " + headEp.getPath();
            if (!baseEndpoints.containsKey(key)) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.LOW)
                        .score(10)
                        .confidence(1.0)
                        .reasons(List.of("New endpoint added: " + key + ". Existing consumers are not impacted."))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.ENDPOINT_ADDED)
                        .severity(ChangeSeverity.NON_BREAKING)
                        .impact(impact)
                        .description("New endpoint added: " + key)
                        .path(headEp.getPath())
                        .method(headEp.getMethod())
                        .oldValue("Missing")
                        .newValue("Present (" + headEp.getMethod() + " " + headEp.getPath() + ")")
                        .build());
            }
        }
    }
}
