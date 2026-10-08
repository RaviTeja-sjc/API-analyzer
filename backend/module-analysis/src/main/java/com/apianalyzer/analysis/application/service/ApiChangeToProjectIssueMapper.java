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

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", apiChange.getMethod());
        metadata.put("path", apiChange.getPath());
        metadata.put("oldValue", apiChange.getOldValue());
        metadata.put("newValue", apiChange.getNewValue());
        metadata.put("breakingStatus", apiChange.getImpact() != null && apiChange.getImpact().getLevel() != null ? apiChange.getImpact().getLevel().name() : "NON_BREAKING");

        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            // Ignore for now
        }

        return ProjectIssue.builder()
                .projectId(projectId)
                .analysisJobId(analysisJobId)
                .category("API")
                .ruleId(ruleId)
                .severity(apiChange.getSeverity() != null ? apiChange.getSeverity().name() : "INFO")
                .confidence("1.0") // High confidence from direct diffing
                .title(ruleId + " in " + apiChange.getPath())
                .problemDescription(apiChange.getDescription())
                .filePath(apiFilePath != null ? apiFilePath : "Unknown API Specification File")
                .startLine(null)
                .endLine(null)
                .evidence("Detected via OpenAPI structural comparison.")
                .impact(apiChange.getImpact() != null && apiChange.getImpact().getLevel() != null ? apiChange.getImpact().getLevel().name() : "Unknown")
                .metadata(metadataJson)
                .build();
    }
}
