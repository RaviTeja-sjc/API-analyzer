package com.apianalyzer.analysis.application.service;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ApiChangeToProjectIssueMapper {

    private final ObjectMapper objectMapper;

    public ApiChangeToProjectIssueMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProjectIssue map(ApiChange apiChange, UUID projectId, UUID analysisJobId, String apiFilePath) {
        String ruleId = "API-" + apiChange.getType().name();
        String breakingStatus = (apiChange.getSeverity() != null) ? apiChange.getSeverity().name() : "NON_BREAKING";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", apiChange.getMethod());
        metadata.put("path", apiChange.getPath());
        metadata.put("oldValue", apiChange.getOldValue());
        metadata.put("newValue", apiChange.getNewValue());
        metadata.put("breakingStatus", breakingStatus);
        metadata.put("severity", breakingStatus);
        metadata.put("impactedConsumers", apiChange.getImpactedConsumers());

        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception ignored) {}

        String evidence = "Detected via OpenAPI structural comparison (" + apiChange.getMethod() + " " + apiChange.getPath() + ")";
        Integer startLine = null;
        String filePath = apiFilePath != null ? apiFilePath : "openapi.yaml";

        if (!apiChange.getImpactedConsumers().isEmpty()) {
            var primaryConsumer = apiChange.getImpactedConsumers().get(0);
            evidence += " -> Handled by " + primaryConsumer.getName() + " (" + primaryConsumer.getFilePath() + ":" + primaryConsumer.getLineNumber() + ")";
            if (primaryConsumer.getLineNumber() != null) {
                startLine = primaryConsumer.getLineNumber();
            }
        }

        return ProjectIssue.builder()
                .projectId(projectId)
                .analysisJobId(analysisJobId)
                .category("API")
                .ruleId(ruleId)
                .severity(breakingStatus.equals("BREAKING") ? "HIGH" : (breakingStatus.equals("POTENTIALLY_BREAKING") ? "MEDIUM" : "LOW"))
                .confidence("1.0")
                .title(apiChange.getDescription())
                .problemDescription(apiChange.getDescription())
                .filePath(filePath)
                .startLine(startLine)
                .endLine(null)
                .evidence(evidence)
                .impact(apiChange.getImpact() != null && apiChange.getImpact().getLevel() != null ? apiChange.getImpact().getLevel().name() : breakingStatus)
                .metadata(metadataJson)
                .build();
    }
}
