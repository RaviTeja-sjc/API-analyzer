package com.apianalyzer.analysis.application.service.rules;

import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Parameter;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ParameterRule implements DiffRule {
    @Override
    public String getName() { return "Parameter Rule"; }

    @Override
    public void evaluate(DiffContext context) {
        if (context.getBase() == null || context.getHead() == null) return;

        Map<String, Endpoint> headMap = context.getHead().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (e1, e2) -> e1));

        for (Endpoint baseEp : context.getBase().getEndpoints()) {
            String key = baseEp.getMethod() + " " + baseEp.getPath();
            Endpoint headEp = headMap.get(key);
            if (headEp == null) continue; // Handled by EndpointRemovedRule

            Map<String, Parameter> headParams = headEp.getParameters().stream()
                    .collect(Collectors.toMap(p -> p.getName() + "|" + p.getIn(), p -> p, (p1, p2) -> p1));
            Map<String, Parameter> baseParams = baseEp.getParameters().stream()
                    .collect(Collectors.toMap(p -> p.getName() + "|" + p.getIn(), p -> p, (p1, p2) -> p1));

            // 1. Removed parameters and modified parameters
            for (Parameter baseP : baseEp.getParameters()) {
                String pKey = baseP.getName() + "|" + baseP.getIn();
                if (!headParams.containsKey(pKey)) {
                    ImpactClassification impact = ImpactClassification.builder()
                            .level(ImpactClassification.Level.HIGH)
                            .score(80)
                            .confidence(1.0)
                            .reasons(List.of("Parameter '" + baseP.getName() + "' (" + baseP.getIn() + ") was removed from endpoint " + key))
                            .build();

                    context.addChange(ApiChange.builder()
                            .type(ChangeType.PARAMETER_REMOVED)
                            .severity(ChangeSeverity.BREAKING)
                            .impact(impact)
                            .description("Parameter removed: " + baseP.getName() + " (" + baseP.getIn() + ")")
                            .path(baseEp.getPath())
                            .method(baseEp.getMethod())
                            .oldValue("Present (" + baseP.getIn() + ")")
                            .newValue("Removed")
                            .build());
                } else {
                    Parameter headP = headParams.get(pKey);
                    // Became required
                    if (!baseP.isRequired() && headP.isRequired()) {
                        ImpactClassification impact = ImpactClassification.builder()
                                .level(ImpactClassification.Level.HIGH)
                                .score(85)
                                .confidence(1.0)
                                .reasons(List.of("Parameter '" + headP.getName() + "' changed from optional to required. Clients omitting it will fail with 400 Bad Request."))
                                .build();

                        context.addChange(ApiChange.builder()
                                .type(ChangeType.PARAMETER_CHANGED)
                                .severity(ChangeSeverity.BREAKING)
                                .impact(impact)
                                .description("Parameter became required: " + headP.getName())
                                .path(baseEp.getPath())
                                .method(baseEp.getMethod())
                                .oldValue("Optional")
                                .newValue("Required")
                                .build());
                    }

                    // Schema type changed
                    if (baseP.getSchema() != null && headP.getSchema() != null) {
                        String baseType = baseP.getSchema().getType();
                        String headType = headP.getSchema().getType();
                        if (baseType != null && headType != null && !baseType.equalsIgnoreCase(headType)) {
                            ImpactClassification impact = ImpactClassification.builder()
                                    .level(ImpactClassification.Level.HIGH)
                                    .score(85)
                                    .confidence(1.0)
                                    .reasons(List.of("Parameter '" + headP.getName() + "' schema type changed from " + baseType + " to " + headType))
                                    .build();

                            context.addChange(ApiChange.builder()
                                    .type(ChangeType.SCHEMA_TYPE_CHANGED)
                                    .severity(ChangeSeverity.BREAKING)
                                    .impact(impact)
                                    .description("Parameter type mutated: " + headP.getName() + " (" + baseType + " -> " + headType + ")")
                                    .path(baseEp.getPath())
                                    .method(baseEp.getMethod())
                                    .oldValue(baseType)
                                    .newValue(headType)
                                    .build());
                        }
                    }
                }
            }

            // 2. Newly added parameters in HEAD
            for (Parameter headP : headEp.getParameters()) {
                String pKey = headP.getName() + "|" + headP.getIn();
                if (!baseParams.containsKey(pKey)) {
                    if (headP.isRequired()) {
                        ImpactClassification impact = ImpactClassification.builder()
                                .level(ImpactClassification.Level.HIGH)
                                .score(90)
                                .confidence(1.0)
                                .reasons(List.of("Newly required parameter '" + headP.getName() + "' (" + headP.getIn() + ") added. Downstream clients not sending this parameter will receive HTTP 400."))
                                .build();

                        context.addChange(ApiChange.builder()
                                .type(ChangeType.PARAMETER_ADDED)
                                .severity(ChangeSeverity.BREAKING)
                                .impact(impact)
                                .description("Newly required parameter added: " + headP.getName() + " (" + headP.getIn() + ")")
                                .path(headEp.getPath())
                                .method(headEp.getMethod())
                                .oldValue("Absent")
                                .newValue("Required (" + headP.getIn() + ")")
                                .build());
                    } else {
                        ImpactClassification impact = ImpactClassification.builder()
                                .level(ImpactClassification.Level.LOW)
                                .score(5)
                                .confidence(1.0)
                                .reasons(List.of("New optional parameter '" + headP.getName() + "' added. Compatible with existing callers."))
                                .build();

                        context.addChange(ApiChange.builder()
                                .type(ChangeType.PARAMETER_ADDED)
                                .severity(ChangeSeverity.NON_BREAKING)
                                .impact(impact)
                                .description("Optional parameter added: " + headP.getName())
                                .path(headEp.getPath())
                                .method(headEp.getMethod())
                                .oldValue("Absent")
                                .newValue("Optional (" + headP.getIn() + ")")
                                .build());
                    }
                }
            }
        }
    }
}
