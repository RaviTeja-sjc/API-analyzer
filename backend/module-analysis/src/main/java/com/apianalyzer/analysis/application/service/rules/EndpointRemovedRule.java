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
public class EndpointRemovedRule implements DiffRule {
    @Override
    public String getName() { return "Endpoint Removed Rule"; }

    @Override
    public void evaluate(DiffContext context) {
        if (context.getBase() == null || context.getHead() == null) return;

        Map<String, Endpoint> headEndpoints = context.getHead().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (e1, e2) -> e1));

        for (Endpoint baseEp : context.getBase().getEndpoints()) {
            String key = baseEp.getMethod() + " " + baseEp.getPath();
            if (!headEndpoints.containsKey(key)) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.CRITICAL)
                        .score(95)
                        .confidence(1.0)
                        .reasons(List.of("Endpoint " + key + " was removed completely. Any downstream client calling this endpoint will receive HTTP 404."))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.ENDPOINT_REMOVED)
                        .severity(ChangeSeverity.BREAKING)
                        .impact(impact)
                        .description("Endpoint removed: " + key)
                        .path(baseEp.getPath())
                        .method(baseEp.getMethod())
                        .oldValue("Present (" + baseEp.getMethod() + " " + baseEp.getPath() + ")")
                        .newValue("Removed")
                        .build());
            }
        }
    }
}
