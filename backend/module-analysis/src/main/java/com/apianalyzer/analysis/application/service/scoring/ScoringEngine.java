package com.apianalyzer.analysis.application.service.scoring;

import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoringEngine {

    private final ProjectIssueRepository projectIssueRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void calculateAndPersistScores(UUID analysisJobId, boolean apiAnalysisPerformed) {
        AnalysisJob job = analysisJobRepository.findById(analysisJobId).orElseThrow();
        List<ProjectIssue> issues = projectIssueRepository.findByAnalysisJobId(analysisJobId);

        ScoreBreakdown.SecurityScore securityScore = calculateSecurityScore(issues);
        ScoreBreakdown.ApiHealthScore apiScore = calculateApiHealthScore(issues, apiAnalysisPerformed);

        ScoreBreakdown breakdown = ScoreBreakdown.builder()
                .security(securityScore)
                .apiHealth(apiScore)
                .build();

        job.setSecurityScore(securityScore.getFinalScore());
        job.setApiHealthScore(apiScore.getFinalScore());
        
        try {
            job.setScoreBreakdown(objectMapper.writeValueAsString(breakdown));
        } catch (Exception e) {
            // Log serialization error
        }

        analysisJobRepository.save(job);
    }

    private ScoreBreakdown.SecurityScore calculateSecurityScore(List<ProjectIssue> allIssues) {
        List<ProjectIssue> secIssues = allIssues.stream()
                .filter(i -> "SECURITY".equalsIgnoreCase(i.getCategory()))
                .collect(Collectors.toList());

        Map<String, Integer> severityCount = new HashMap<>();
        double totalPenalty = 0.0;

        for (ProjectIssue issue : secIssues) {
            String sev = issue.getSeverity() != null ? issue.getSeverity().toUpperCase() : "INFO";
            severityCount.put(sev, severityCount.getOrDefault(sev, 0) + 1);

            int severityWeight = switch (sev) {
                case "CRITICAL" -> 30;
                case "HIGH" -> 15;
                case "MEDIUM" -> 5;
                case "LOW" -> 1;
                default -> 0;
            };

            double confidenceWeight = 1.0;
            if (issue.getConfidence() != null) {
                try {
                    confidenceWeight = Double.parseDouble(issue.getConfidence());
                } catch (NumberFormatException ignored) {}
            }

            totalPenalty += (severityWeight * confidenceWeight);
        }

        int finalScore = Math.max(0, 100 - (int) Math.round(totalPenalty));

        return ScoreBreakdown.SecurityScore.builder()
                .baseScore(100)
                .finalScore(finalScore)
                .findingCount(secIssues.size())
                .severityBreakdown(severityCount)
                .totalPenalty((int) Math.round(totalPenalty))
                .build();
    }

    private ScoreBreakdown.ApiHealthScore calculateApiHealthScore(List<ProjectIssue> allIssues, boolean apiAnalysisPerformed) {
        if (!apiAnalysisPerformed) {
            return ScoreBreakdown.ApiHealthScore.builder()
                    .status("UNSUPPORTED")
                    .baseScore(null)
                    .finalScore(null)
                    .findingCount(0)
                    .breakingFindingCount(0)
                    .impactBreakdown(new HashMap<>())
                    .totalPenalty(0)
                    .build();
        }

        List<ProjectIssue> apiIssues = allIssues.stream()
                .filter(i -> "API".equalsIgnoreCase(i.getCategory()))
                .collect(Collectors.toList());

        Map<String, Integer> impactCount = new HashMap<>();
        double totalPenalty = 0.0;
        int breakingCount = 0;

        for (ProjectIssue issue : apiIssues) {
            String impact = issue.getImpact() != null ? issue.getImpact().toUpperCase() : "UNKNOWN";
            impactCount.put(impact, impactCount.getOrDefault(impact, 0) + 1);

            if ("BREAKING".equals(impact)) {
                totalPenalty += 20;
                breakingCount++;
            } else if ("POTENTIALLY_BREAKING".equals(impact)) {
                totalPenalty += 5;
            } else if ("NON_BREAKING".equals(impact)) {
                totalPenalty += 1;
            }
        }

        int finalScore = Math.max(0, 100 - (int) Math.round(totalPenalty));

        return ScoreBreakdown.ApiHealthScore.builder()
                .status("ANALYZED")
                .baseScore(100)
                .finalScore(finalScore)
                .findingCount(apiIssues.size())
                .breakingFindingCount(breakingCount)
                .impactBreakdown(impactCount)
                .totalPenalty((int) Math.round(totalPenalty))
                .build();
    }
}
