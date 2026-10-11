package com.apianalyzer.analysis.application.service.rules;

import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Endpoint;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel.Response;
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
public class ResponseRule implements DiffRule {
    @Override
    public String getName() { return "Response Rule"; }

    @Override
    public void evaluate(DiffContext context) {
        if (context.getBase() == null || context.getHead() == null) return;

        Map<String, Endpoint> headMap = context.getHead().getEndpoints().stream()
                .collect(Collectors.toMap(e -> e.getMethod() + " " + e.getPath(), e -> e, (e1, e2) -> e1));

        for (Endpoint baseEp : context.getBase().getEndpoints()) {
            String key = baseEp.getMethod() + " " + baseEp.getPath();
            Endpoint headEp = headMap.get(key);
            if (headEp == null) continue;

            Map<String, Response> baseResponses = baseEp.getResponses();
            Map<String, Response> headResponses = headEp.getResponses();

            for (Map.Entry<String, Response> baseRespEntry : baseResponses.entrySet()) {
                String status = baseRespEntry.getKey();
                Response baseResp = baseRespEntry.getValue();

                if (!headResponses.containsKey(status)) {
                    // Removing a success response (2xx) is breaking
                    boolean isSuccess = status.startsWith("2");
                    ChangeSeverity severity = isSuccess ? ChangeSeverity.BREAKING : ChangeSeverity.POTENTIALLY_BREAKING;
                    ImpactClassification impact = ImpactClassification.builder()
                            .level(isSuccess ? ImpactClassification.Level.HIGH : ImpactClassification.Level.MEDIUM)
                            .score(isSuccess ? 85 : 50)
                            .confidence(1.0)
                            .reasons(List.of("HTTP response code " + status + " removed from " + key + "."))
                            .build();

                    context.addChange(ApiChange.builder()
                            .type(ChangeType.RESPONSE_REMOVED)
                            .severity(severity)
                            .impact(impact)
                            .description("Response status " + status + " removed on " + key)
                            .path(baseEp.getPath())
                            .method(baseEp.getMethod())
                            .oldValue("HTTP " + status)
                            .newValue("Removed")
                            .build());
                } else {
                    Response headResp = headResponses.get(status);
                    compareResponseContent(context, baseEp, status, baseResp, headResp);
                }
            }
        }
    }

    private void compareResponseContent(DiffContext context, Endpoint ep, String status, Response baseResp, Response headResp) {
        if (baseResp == null || headResp == null) return;

        for (Map.Entry<String, Schema> mediaEntry : baseResp.getContent().entrySet()) {
            String mediaType = mediaEntry.getKey();
            Schema baseSchema = mediaEntry.getValue();

            if (!headResp.getContent().containsKey(mediaType)) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.HIGH)
                        .score(80)
                        .confidence(1.0)
                        .reasons(List.of("Response media type '" + mediaType + "' removed from " + ep.getMethod() + " " + ep.getPath() + " for status " + status))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.RESPONSE_CHANGED)
                        .severity(ChangeSeverity.BREAKING)
                        .impact(impact)
                        .description("Response media type removed: " + mediaType + " (HTTP " + status + ")")
                        .path(ep.getPath())
                        .method(ep.getMethod())
                        .oldValue("Supported (" + mediaType + ")")
                        .newValue("Removed")
                        .build());
            } else {
                Schema headSchema = headResp.getContent().get(mediaType);
                compareResponseSchema(context, ep, status, mediaType, baseSchema, headSchema);
            }
        }
    }

    private void compareResponseSchema(DiffContext context, Endpoint ep, String status, String mediaType, Schema baseSchema, Schema headSchema) {
        // Check top-level schema enum values
        if (!baseSchema.getEnumValues().isEmpty()) {
            for (String oldEnum : baseSchema.getEnumValues()) {
                if (!headSchema.getEnumValues().contains(oldEnum)) {
                    ImpactClassification impact = ImpactClassification.builder()
                            .level(ImpactClassification.Level.HIGH)
                            .score(80)
                            .confidence(1.0)
                            .reasons(List.of("Enum value '" + oldEnum + "' removed from response schema for " + ep.getMethod() + " " + ep.getPath() + " (HTTP " + status + ")."))
                            .build();

                    context.addChange(ApiChange.builder()
                            .type(ChangeType.ENUM_VALUE_REMOVED)
                            .severity(ChangeSeverity.BREAKING)
                            .impact(impact)
                            .description("Enum value removed from response: " + oldEnum)
                            .path(ep.getPath())
                            .method(ep.getMethod())
                            .oldValue(oldEnum)
                            .newValue("Removed")
                            .build());
                }
            }
        }

        // 1. Removed response property: BREAKING for consumers expecting the field
        for (Map.Entry<String, Schema> basePropEntry : baseSchema.getProperties().entrySet()) {
            String propName = basePropEntry.getKey();
            Schema baseProp = basePropEntry.getValue();

            if (!headSchema.getProperties().containsKey(propName)) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.HIGH)
                        .score(85)
                        .confidence(1.0)
                        .reasons(List.of("Response field '" + propName + "' was removed from " + ep.getMethod() + " " + ep.getPath() + " (HTTP " + status + "). Downstream consumers relying on this property will receive undefined/null."))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.SCHEMA_PROPERTY_REMOVED)
                        .severity(ChangeSeverity.BREAKING)
                        .impact(impact)
                        .description("Response property removed: " + propName + " (HTTP " + status + ")")
                        .path(ep.getPath())
                        .method(ep.getMethod())
                        .oldValue("Present (" + (baseProp != null ? baseProp.getType() : "any") + ")")
                        .newValue("Removed")
                        .build());
            } else {
                Schema headProp = headSchema.getProperties().get(propName);
                if (baseProp != null && headProp != null) {
                    // Incompatible type mutation
                    if (baseProp.getType() != null && headProp.getType() != null && !baseProp.getType().equalsIgnoreCase(headProp.getType())) {
                        ImpactClassification impact = ImpactClassification.builder()
                                .level(ImpactClassification.Level.HIGH)
                                .score(90)
                                .confidence(1.0)
                                .reasons(List.of("Response field '" + propName + "' type mutated from " + baseProp.getType() + " to " + headProp.getType() + ". Downstream deserializers will fail."))
                                .build();

                        context.addChange(ApiChange.builder()
                                .type(ChangeType.SCHEMA_TYPE_CHANGED)
                                .severity(ChangeSeverity.BREAKING)
                                .impact(impact)
                                .description("Response property type changed: " + propName + " (" + baseProp.getType() + " -> " + headProp.getType() + ")")
                                .path(ep.getPath())
                                .method(ep.getMethod())
                                .oldValue(baseProp.getType())
                                .newValue(headProp.getType())
                                .build());
                    }

                    // Enum value removed
                    if (!baseProp.getEnumValues().isEmpty()) {
                        for (String oldEnum : baseProp.getEnumValues()) {
                            if (!headProp.getEnumValues().contains(oldEnum)) {
                                ImpactClassification impact = ImpactClassification.builder()
                                        .level(ImpactClassification.Level.MEDIUM)
                                        .score(70)
                                        .confidence(0.9)
                                        .reasons(List.of("Enum value '" + oldEnum + "' removed from property '" + propName + "' in response."))
                                        .build();

                                context.addChange(ApiChange.builder()
                                        .type(ChangeType.ENUM_VALUE_REMOVED)
                                        .severity(ChangeSeverity.POTENTIALLY_BREAKING)
                                        .impact(impact)
                                        .description("Enum value removed from response: " + oldEnum + " on " + propName)
                                        .path(ep.getPath())
                                        .method(ep.getMethod())
                                        .oldValue("Enum contains: " + oldEnum)
                                        .newValue("Value dropped")
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        // 2. Added response property: NON_BREAKING
        for (Map.Entry<String, Schema> headPropEntry : headSchema.getProperties().entrySet()) {
            String propName = headPropEntry.getKey();
            if (!baseSchema.getProperties().containsKey(propName)) {
                ImpactClassification impact = ImpactClassification.builder()
                        .level(ImpactClassification.Level.LOW)
                        .score(5)
                        .confidence(1.0)
                        .reasons(List.of("New optional response property '" + propName + "' added. Compatible with existing callers."))
                        .build();

                context.addChange(ApiChange.builder()
                        .type(ChangeType.SCHEMA_PROPERTY_ADDED)
                        .severity(ChangeSeverity.NON_BREAKING)
                        .impact(impact)
                        .description("Response property added: " + propName + " (HTTP " + status + ")")
                        .path(ep.getPath())
                        .method(ep.getMethod())
                        .oldValue("Absent")
                        .newValue("Present")
                        .build());
            }
        }
    }
}
