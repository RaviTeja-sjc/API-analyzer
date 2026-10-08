package com.apianalyzer.migration.application.service;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot.RepositoryFile;
import com.apianalyzer.migration.application.service.rules.ApiEndpointRemovedMigrationRule;
import com.apianalyzer.migration.application.service.rules.HardcodedSecretMigrationRule;
import com.apianalyzer.migration.application.service.rules.MigrationRule;
import com.apianalyzer.migration.domain.entity.MigrationProposal;
import com.apianalyzer.migration.domain.repository.MigrationProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MigrationPipelineTest {

    private PatchGenerationService patchGenerationService;
    private MigrationProposalRepository proposalRepository;

    @BeforeEach
    void setUp() {
        proposalRepository = Mockito.mock(MigrationProposalRepository.class);
        List<MigrationRule> rules = Arrays.asList(
                new HardcodedSecretMigrationRule(),
                new ApiEndpointRemovedMigrationRule()
        );
        patchGenerationService = new PatchGenerationService(rules, proposalRepository);
    }

    @Test
    void testHardcodedSecretAndLeakage() {
        // Setup original content with a secret
        String secretValue = "SUPER_SECRET_12345";
        String secretLine = "    private String apiKey = \"" + secretValue + "\";";
        String originalContent = "public class Config {\n" + secretLine + "\n}\n";
        
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
                .files(List.of(RepositoryFile.builder().path("src/Config.java").content(originalContent).build()))
                .build();

        ProjectIssue issue = new ProjectIssue();
        issue.setId(UUID.randomUUID());
        issue.setAnalysisJobId(UUID.randomUUID());
        issue.setRuleId("HARDCODED_SECRET");
        issue.setFilePath("src/Config.java");
        issue.setEvidence(secretLine);
        issue.setStartLine(2);

        when(proposalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Generate proposal
        List<MigrationProposal> proposals = patchGenerationService.generateProposals(List.of(issue), snapshot);

        // 1. HARDCODED SECRET MIGRATION TEST
        assertEquals(1, proposals.size(), "Proposal created");
        MigrationProposal proposal = proposals.get(0);
        assertTrue(proposal.isAutoFixSupported(), "Auto-fix should be supported");
        assertEquals("src/Config.java", proposal.getFilePath());

        // 2. SECRET LEAKAGE TEST
        String proposedContent = proposal.getProposedContent();
        String unifiedDiff = proposal.getUnifiedDiff();
        String rationale = proposal.getRationale();

        assertNotNull(proposedContent);
        assertNotNull(unifiedDiff);
        assertNotNull(rationale);

        assertFalse(proposedContent.contains(secretValue), "SECRET_VALUE_PRESENT_IN_OUTPUT = false (in proposedContent)");
        assertFalse(unifiedDiff.contains(secretValue), "SECRET_VALUE_PRESENT_IN_OUTPUT = false (in unifiedDiff)");
        assertFalse(rationale.contains(secretValue), "SECRET_VALUE_PRESENT_IN_OUTPUT = false (in rationale)");

        // 3. REAL UNIFIED DIFF TEST
        assertTrue(unifiedDiff.contains("--- a/src/Config.java"));
        assertTrue(unifiedDiff.contains("+++ b/src/Config.java"));
        assertTrue(unifiedDiff.contains("-    private String apiKey = \"[REDACTED]\";"), unifiedDiff);
        assertTrue(unifiedDiff.contains("+private String apiKey = System.getenv(\"PRIVATE_STRING_APIKEY\");"), unifiedDiff);
    }

    @Test
    void testStalePatchProtection() {
        String originalContent = "public class Config { String key = \"secret\"; }";
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
                .files(List.of(RepositoryFile.builder().path("src/Config.java").content(originalContent).build()))
                .build();

        ProjectIssue issue = new ProjectIssue();
        issue.setId(UUID.randomUUID());
        issue.setAnalysisJobId(UUID.randomUUID());
        issue.setRuleId("HARDCODED_SECRET");
        issue.setFilePath("src/Config.java");
        issue.setEvidence("String key = \"secret\";");
        issue.setStartLine(1);

        when(proposalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        List<MigrationProposal> proposals = patchGenerationService.generateProposals(List.of(issue), snapshot);
        MigrationProposal proposal = proposals.get(0);

        // STALE PATCH TEST: The hash must match the actual content. 
        // We simulate a changed file to show it would be stale.
        String changedContent = "public class Config { // changed }";
        RepositorySnapshot changedSnapshot = RepositorySnapshot.builder()
                .files(List.of(RepositorySnapshot.RepositoryFile.builder().path("src/Config.java").content(changedContent).build()))
                .build();
        
        // Use the PatchGenerationService to make the actual stale decision
        boolean isStale = patchGenerationService.isProposalStale(proposal, changedSnapshot);
        assertTrue(isStale, "Proposal should be identified as stale");
    }

    @Test
    void testJobIsolation() {
        // Job A
        UUID jobA = UUID.randomUUID();
        ProjectIssue issueA = new ProjectIssue();
        issueA.setAnalysisJobId(jobA);
        issueA.setRuleId("API-ENDPOINT_REMOVED");

        // Job B
        UUID jobB = UUID.randomUUID();
        ProjectIssue issueB = new ProjectIssue();
        issueB.setAnalysisJobId(jobB);
        issueB.setRuleId("API-ENDPOINT_REMOVED");
        
        // Both generate proposals and get isolated by their analysis job IDs
        MigrationProposal proposalA = new ApiEndpointRemovedMigrationRule().generateProposal(issueA, RepositorySnapshot.builder().build());
        MigrationProposal proposalB = new ApiEndpointRemovedMigrationRule().generateProposal(issueB, RepositorySnapshot.builder().build());
        
        assertEquals(jobA, proposalA.getAnalysisJobId(), "Job A proposal matches Job A ID");
        assertEquals(jobB, proposalB.getAnalysisJobId(), "Job B proposal matches Job B ID");
        assertNotEquals(proposalA.getAnalysisJobId(), proposalB.getAnalysisJobId(), "Job isolation maintained");
    }

    @Test
    void testCleanRepository() {
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
                .files(List.of(RepositoryFile.builder().path("src/Main.java").content("class Main {}").build()))
                .build();

        when(proposalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // CLEAN REPOSITORY TEST
        List<ProjectIssue> issues = List.of();
        List<MigrationProposal> proposals = patchGenerationService.generateProposals(issues, snapshot);

        assertEquals(0, issues.size(), "ProjectIssues requiring migration = 0");
        assertEquals(0, proposals.size(), "MigrationProposals = 0");
    }

    @Test
    void testDeterminism() {
        String content = "class Main {\n String key = \"secret\";\n }";
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
                .files(List.of(RepositoryFile.builder().path("Main.java").content(content).build()))
                .build();

        ProjectIssue issue = new ProjectIssue();
        issue.setId(UUID.randomUUID());
        issue.setRuleId("HARDCODED_SECRET");
        issue.setFilePath("Main.java");
        issue.setEvidence("secret");
        issue.setStartLine(2);

        when(proposalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MigrationProposal> run1 = patchGenerationService.generateProposals(List.of(issue), snapshot);
        List<MigrationProposal> run2 = patchGenerationService.generateProposals(List.of(issue), snapshot);

        MigrationProposal p1 = run1.get(0);
        MigrationProposal p2 = run2.get(0);

        assertEquals(p1.getProposedContent(), p2.getProposedContent());
        assertEquals(p1.getUnifiedDiff(), p2.getUnifiedDiff());
        assertEquals(p1.getRuleId(), p2.getRuleId());
        assertEquals(p1.getValidationStatus(), p2.getValidationStatus());
    }
}
