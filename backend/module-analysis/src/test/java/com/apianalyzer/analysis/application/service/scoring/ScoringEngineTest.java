package com.apianalyzer.analysis.application.service.scoring;

import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ScoringEngineTest {

    private ProjectIssueRepository projectIssueRepository;
    private AnalysisJobRepository analysisJobRepository;
    private ScoringEngine scoringEngine;
    private UUID jobId;
    private AnalysisJob job;

    @BeforeEach
    void setUp() {
        projectIssueRepository = Mockito.mock(ProjectIssueRepository.class);
        analysisJobRepository = Mockito.mock(AnalysisJobRepository.class);
        scoringEngine = new ScoringEngine(projectIssueRepository, analysisJobRepository, new ObjectMapper());
        
        jobId = UUID.randomUUID();
        job = new AnalysisJob();
        job.setId(jobId);
        
        when(analysisJobRepository.findById(jobId)).thenReturn(Optional.of(job));
    }

    @Test
    void security_noFindings_score100() {
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of());
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(100, job.getSecurityScore());
    }

    @Test
    void security_criticalFinding_scoreDecreases() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("SECURITY");
        issue.setSeverity("CRITICAL");
        issue.setConfidence("1.0");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(70, job.getSecurityScore()); // 100 - 30*1.0
    }

    @Test
    void security_highIsLessSevereThanCritical() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("SECURITY");
        issue.setSeverity("HIGH");
        issue.setConfidence("1.0");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(85, job.getSecurityScore()); // 100 - 15*1.0
    }

    @Test
    void security_lowerConfidence_lowerPenalty() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("SECURITY");
        issue.setSeverity("CRITICAL");
        issue.setConfidence("0.5");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(85, job.getSecurityScore()); // 100 - 30*0.5
    }

    @Test
    void security_multipleFindingsAccumulate() {
        ProjectIssue i1 = new ProjectIssue();
        i1.setCategory("SECURITY");
        i1.setSeverity("HIGH");
        i1.setConfidence("1.0");
        
        ProjectIssue i2 = new ProjectIssue();
        i2.setCategory("SECURITY");
        i2.setSeverity("MEDIUM");
        i2.setConfidence("1.0");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(i1, i2));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(80, job.getSecurityScore()); // 100 - 15 - 5
    }

    @Test
    void security_extremeFindings_clampToZero() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("SECURITY");
        issue.setSeverity("CRITICAL");
        issue.setConfidence("1.0");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue, issue, issue, issue, issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(0, job.getSecurityScore()); // 100 - 150 -> clamped to 0
    }

    @Test
    void security_nonSecurityDoesNotAffect() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("API");
        issue.setSeverity("CRITICAL");
        issue.setImpact("BREAKING");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(100, job.getSecurityScore()); // unaffected
    }

    @Test
    void api_noFindings_score100() {
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of());
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(100, job.getApiHealthScore());
    }

    @Test
    void api_breakingFinding_decreasesHealth() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("API");
        issue.setImpact("BREAKING");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(80, job.getApiHealthScore()); // 100 - 20
    }

    @Test
    void api_nonBreakingFinding_smallPenalty() {
        ProjectIssue issue = new ProjectIssue();
        issue.setCategory("API");
        issue.setImpact("NON_BREAKING");
        
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of(issue));
        scoringEngine.calculateAndPersistScores(jobId, true);
        
        assertEquals(99, job.getApiHealthScore()); // 100 - 1
    }

    @Test
    void api_unsupported_scoreIsNull() {
        when(projectIssueRepository.findByAnalysisJobId(jobId)).thenReturn(List.of());
        scoringEngine.calculateAndPersistScores(jobId, false);
        
        assertNull(job.getApiHealthScore());
    }

    @Test
    void isolation_findingsAreScopedToJob() {
        // By definition of passing jobId to findByAnalysisJobId, findings from A don't affect B.
        // verified through Mockito structure
        scoringEngine.calculateAndPersistScores(jobId, true);
        Mockito.verify(projectIssueRepository).findByAnalysisJobId(jobId);
    }
}
