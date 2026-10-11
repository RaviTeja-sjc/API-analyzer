package com.apianalyzer.analysis.application.service;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ApiChangeToProjectIssueMapperTest {

    private ApiChangeToProjectIssueMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ApiChangeToProjectIssueMapper(new ObjectMapper());
    }

    @Test
    void testMapper_PreservesRealSeverityAndConfidence() {
        ApiChange change = ApiChange.builder()
                .type(ChangeType.PARAMETER_REMOVED)
                .severity(ChangeSeverity.BREAKING)
                .description("Required parameter 'page' was removed.")
                .path("/users/{id}")
                .method("GET")
                .oldValue("page")
                .newValue(null)
                .impact(ImpactClassification.builder().level(ImpactClassification.Level.CRITICAL).build())
                .build();

        UUID projectId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ProjectIssue issue = mapper.map(change, projectId, jobId, "docs/openapi.yaml");

        assertEquals(projectId, issue.getProjectId());
        assertEquals(jobId, issue.getAnalysisJobId());
        assertEquals("API", issue.getCategory());
        assertEquals("API-PARAMETER_REMOVED", issue.getRuleId());
        assertEquals("HIGH", issue.getSeverity());
        assertEquals("1.0", issue.getConfidence());
        assertTrue(issue.getTitle().contains("Required parameter 'page' was removed."));
        assertEquals("Required parameter 'page' was removed.", issue.getProblemDescription());
        assertEquals("docs/openapi.yaml", issue.getFilePath());
        assertNull(issue.getStartLine());
        assertNull(issue.getEndLine());
        assertTrue(issue.getEvidence().contains("Detected via OpenAPI structural comparison"));
        assertNotNull(issue.getMetadata());
        assertTrue(issue.getMetadata().contains("\"breakingStatus\":\"BREAKING\""));
        assertTrue(issue.getMetadata().contains("\"method\":\"GET\""));
    }
}
