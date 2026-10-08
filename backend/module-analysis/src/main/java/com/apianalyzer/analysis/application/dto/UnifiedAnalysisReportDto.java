package com.apianalyzer.analysis.application.dto;


import com.apianalyzer.core.domain.entity.ProjectIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedAnalysisReportDto {
    private JobInfo analysisJob;
    private CoverageInfo coverage;
    private Map<String, Object> scoring;
    private List<ProjectIssue> issues;
    private List<RecommendationDto> recommendations;
    private Map<String, Object> graph;
    private ApiAnalysisInfo apiAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobInfo {
        private String id;
        private String status;
        private Integer progress;
        private String logs;
        private String createdAt;
        private String branch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageInfo {
        private int totalFiles;
        private int analyzedFiles;
        private int skippedFiles;
        private int unsupportedFiles;
        private Map<String, Integer> detectedLanguages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiAnalysisInfo {
        private String baseVersion;
        private String headVersion;
        private Map<String, Object> summary;
        private List<Map<String, Object>> changes;
    }
}
