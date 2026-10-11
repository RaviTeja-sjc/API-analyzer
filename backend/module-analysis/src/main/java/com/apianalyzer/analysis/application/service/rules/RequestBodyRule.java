package com.apianalyzer.analysis.application.service.rules;

import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.RequestBody;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Schema;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RequestBodyRule implements DiffRule {
    @Override
    public String getName() { return "Request Body Rule"; }

    @Override
    public void evaluate(DiffContext context) {
        if (context.getBase() == null || context.getHead() == null) return;

        Map<String, Endpoint> headMap = context.getHead().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (e1, e2) -> e1));

        for (Endpoint baseEp : context.getBase().getEndpoints()) {
            String key = baseEp.getMethod() + " " + baseEp.getPath();
            Endpoint headEp = headMap.get(key);
            if (headEp == null) continue;

            RequestBody baseRb = baseEp.getRequestBody();
            RequestBody headRb = headEp.getRequestBody();

            // 1. Request body became required or added as required
            if (baseRb == null && headRb != null && headRb.isRequired()) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.CRITICAL)
                        .score(90)
                        .confidence(1.0)
                        .reasons(List.of("A mandatory request body was added to " + key + ". Callers sending no payload will be rejected."))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.REQUEST_BODY_CHANGED)
                        .severity(ChangeSeverity.BREAKING)
                        .impact(impact)
                        .description("Request body became required on " + key)
                        .path(baseEp.getPath())
                        .method(baseEp.getMethod())
                        .oldValue("No body required")
                        .newValue("Mandatory request body")
                        .build());
            } else if (baseRb != null && headRb != null) {
                if (!baseRb.isRequired() && headRb.isRequired()) {
                    ImpactClassification impact = ImpactClassification.builder()
                            .level(ImpactClassification.Level.CRITICAL)
                            .score(90)
                            .confidence(1.0)
                            .reasons(List.of("Request body on " + key + " changed from optional to required."))
                            .build();

                    context.addChange(ApiChange.builder()
                            .type(ChangeType.REQUEST_BODY_CHANGED)
                            .severity(ChangeSeverity.BREAKING)
                            .impact(impact)
                            .description("Request body became required on " + key)
                            .path(baseEp.getPath())
                            .method(baseEp.getMethod())
                            .oldValue("Optional")
                            .newValue("Required")
                            .build());
                }

                // Check supported media types
                for (String baseMediaType : baseRb.getContent().keySet()) {
                    if (!headRb.getContent().containsKey(baseMediaType)) {
                        ImpactClassification impact = ImpactClassification.builder()
                                .level(ImpactClassification.Level.HIGH)
                                .score(85)
                                .confidence(1.0)
                                .reasons(List.of("Request content-type '" + baseMediaType + "' removed from " + key))
                                .build();

                        context.addChange(ApiChange.builder()
                                .type(ChangeType.REQUEST_BODY_CHANGED)
                                .severity(ChangeSeverity.BREAKING)
                                .impact(impact)
                                .description("Request media type removed: " + baseMediaType)
                                .path(baseEp.getPath())
                                .method(baseEp.getMethod())
                                .oldValue("Supported (" + baseMediaType + ")")
                                .newValue("Unsupported")
                                .build());
                    } else {
                        // Compare schema properties inside request body
                        Schema baseSchema = baseRb.getContent().get(baseMediaType);
                        Schema headSchema = headRb.getContent().get(baseMediaType);
                        compareSchemaProperties(context, baseEp, baseSchema, headSchema, "Request body [" + baseMediaType + "]");
                    }
                }
            }
        }
    }

    private void compareSchemaProperties(DiffContext context, Endpoint ep, Schema baseSchema, Schema headSchema, String location) {
        if (baseSchema == null || headSchema == null) return;

        // Check required fields in request body schema: newly required field is breaking
        for (String reqField : headSchema.getRequiredFields()) {
            if (!baseSchema.getRequiredFields().contains(reqField)) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.HIGH)
                        .score(85)
                        .confidence(1.0)
                        .reasons(List.of("Field '" + reqField + "' became mandatory in " + location + ". Existing clients omitting it will fail validation."))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.SCHEMA_PROPERTY_ADDED)
                        .severity(ChangeSeverity.BREAKING)
                        .impact(impact)
                        .description("Required request field added: " + reqField + " in " + location)
                        .path(ep.getPath())
                        .method(ep.getMethod())
                        .oldValue("Optional or absent")
                        .newValue("Required field: " + reqField)
                        .build());
            }
        }

        // Compare individual properties
        for (Map.Entry<String, Schema> propEntry : baseSchema.getProperties().entrySet()) {
            String propName = propEntry.getKey();
            Schema baseProp = propEntry.getValue();
            if (headSchema.getProperties().containsKey(propName)) {
                Schema headProp = headSchema.getProperties().get(propName);
                if (baseProp != null && headProp != null) {
                    // Type changed
                    if (baseProp.getType() != null && headProp.getType() != null && !baseProp.getType().equalsIgnoreCase(headProp.getType())) {
                        ImpactClassification impact = ImpactClassification.builder()
                                .level(ImpactClassification.Level.HIGH)
                                .score(85)
                                .confidence(1.0)
                                .reasons(List.of("Property '" + propName + "' in " + location + " type mutated from " + baseProp.getType() + " to " + headProp.getType()))
                                .build();

                        context.addChange(ApiChange.builder()
                                .type(ChangeType.SCHEMA_TYPE_CHANGED)
                                .severity(ChangeSeverity.BREAKING)
                                .impact(impact)
                                .description("Request field type changed: " + propName + " (" + baseProp.getType() + " -> " + headProp.getType() + ")")
                                .path(ep.getPath())
                                .method(ep.getMethod())
                                .oldValue(baseProp.getType())
                                .newValue(headProp.getType())
                                .build());
                    }
                }
            }
        }
    }
}
