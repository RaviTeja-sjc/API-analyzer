package com.apianalyzer.analysis.application.service.recommendation;

import com.apianalyzer.analysis.application.dto.RecommendationDto;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEngineServiceTest {

    private RecommendationEngineService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationEngineService();
    }

    @Test
    void testHardcodedSecretFinding() {
        UUID issueId = UUID.randomUUID();
        ProjectIssue issue = ProjectIssue.builder()
                .id(issueId)
                .ruleId("HARDCODED_SECRET")
                .severity("HIGH")
                .filePath("src/main/resources/application.properties")
                .startLine(10)
                .endLine(10)
                .evidence("api.key=[REDACTED]")
                .build();

        List<RecommendationDto> result = service.generateRecommendations(List.of(issue));

        assertThat(result).hasSize(1);
        RecommendationDto dto = result.get(0);
        assertThat(dto.getProjectIssueId()).isEqualTo(issueId);
        assertThat(dto.getRuleId()).isEqualTo("HARDCODED_SECRET");
        assertThat(dto.getPriority()).isEqualTo("HIGH");
        assertThat(dto.getFilePath()).isEqualTo("src/main/resources/application.properties");
        assertThat(dto.getStartLine()).isEqualTo(10);
        assertThat(dto.getEvidence()).isEqualTo("api.key=[REDACTED]");
        assertThat(dto.getTitle()).isEqualTo("Secure Hardcoded Credentials");
        assertThat(dto.getRemediation()).contains("environment configuration");
        assertThat(dto.getExplanation()).contains("compromises system security");
    }

    @Test
    void testInappropriateLayerCouplingFinding() {
        ProjectIssue issue = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .ruleId("INAPPROPRIATE_LAYER_COUPLING")
                .build();

        List<RecommendationDto> result = service.generateRecommendations(List.of(issue));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Decouple Controller from Repository");
    }

    @Test
    void testApiEndpointRemovalFinding() {
        ProjectIssue issue = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .ruleId("API-ENDPOINT_REMOVED")
                .build();

        List<RecommendationDto> result = service.generateRecommendations(List.of(issue));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("API Endpoint Deprecation");
    }

    @Test
    void testApiParameterRemovalFinding() {
        ProjectIssue issue = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .ruleId("API-PARAMETER_REMOVED")
                .build();

        List<RecommendationDto> result = service.generateRecommendations(List.of(issue));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("API Parameter Deprecation");
    }

    @Test
    void testNoFindings() {
        List<RecommendationDto> result = service.generateRecommendations(new ArrayList<>());
        assertThat(result).isEmpty();
    }

    @Test
    void testNonRelevantFinding() {
        ProjectIssue issue = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .ruleId("SOME_UNKNOWN_RULE")
                .build();

        List<RecommendationDto> result = service.generateRecommendations(List.of(issue));

        assertThat(result).isEmpty();
    }

    @Test
    void testDeterminism() {
        ProjectIssue issue = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .ruleId("HARDCODED_SECRET")
                .build();

        List<RecommendationDto> result1 = service.generateRecommendations(List.of(issue));
        List<RecommendationDto> result2 = service.generateRecommendations(List.of(issue));

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void testIsolation() {
        UUID job1 = UUID.randomUUID();
        UUID job2 = UUID.randomUUID();

        ProjectIssue issue1 = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .analysisJobId(job1)
                .ruleId("HARDCODED_SECRET")
                .build();

        ProjectIssue issue2 = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .analysisJobId(job2)
                .ruleId("INAPPROPRIATE_LAYER_COUPLING")
                .build();

        List<RecommendationDto> result1 = service.generateRecommendations(List.of(issue1));
        List<RecommendationDto> result2 = service.generateRecommendations(List.of(issue2));

        assertThat(result1.get(0).getRuleId()).isEqualTo("HARDCODED_SECRET");
        assertThat(result2.get(0).getRuleId()).isEqualTo("INAPPROPRIATE_LAYER_COUPLING");
    }

    @Test
    void testFixAndReRunBehavior() {
        // Run 1: finding exists
        ProjectIssue issue = ProjectIssue.builder()
                .id(UUID.randomUUID())
                .ruleId("HARDCODED_SECRET")
                .build();
        
        List<RecommendationDto> result1 = service.generateRecommendations(List.of(issue));
        assertThat(result1).hasSize(1);

        // Run 2: finding fixed (empty list passed)
        List<RecommendationDto> result2 = service.generateRecommendations(new ArrayList<>());
        assertThat(result2).isEmpty();
    }
}
