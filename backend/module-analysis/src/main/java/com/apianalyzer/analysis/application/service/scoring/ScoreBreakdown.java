package com.apianalyzer.analysis.application.service.scoring;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ScoreBreakdown {
    private SecurityScore security;
    private ApiHealthScore apiHealth;

    @Data
    @Builder
    public static class SecurityScore {
        private int baseScore;
        private int finalScore;
        private int findingCount;
        private Map<String, Integer> severityBreakdown;
        private int totalPenalty;
    }

    @Data
    @Builder
    public static class ApiHealthScore {
        private String status; // "ANALYZED", "UNSUPPORTED"
        private Integer baseScore;
        private Integer finalScore;
        private Integer findingCount;
        private Integer breakingFindingCount;
        private Map<String, Integer> impactBreakdown;
        private Integer totalPenalty;
    }
}
