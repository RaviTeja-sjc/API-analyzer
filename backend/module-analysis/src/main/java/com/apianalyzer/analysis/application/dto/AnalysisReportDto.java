package com.apianalyzer.analysis.application.dto;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalysisReportDto {
    private String baseVersion;
    private String headVersion;
    private ReportSummary summary;
    @Builder.Default private List<ChangeReport> changes = new ArrayList<>();
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReportSummary {
        private int totalChanges;
        private int breakingChanges;
        private int potentiallyBreakingChanges;
        private int totalImpactedConsumers;
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChangeReport {
        private String endpointMethod;
        private String endpointPath;
        private String changeType;
        private String breakingStatus;
        private String severityLevel; // LOW, HIGH, CRITICAL
        private int impactScore;
        private String description;
        private String oldValue;
        private String newValue;
        
        @Builder.Default private List<String> impactReasons = new ArrayList<>();
        @Builder.Default private List<String> warnings = new ArrayList<>();
        @Builder.Default private List<String> recommendations = new ArrayList<>();
        
        @Builder.Default private List<ConsumerReport> impactedConsumers = new ArrayList<>();
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ConsumerReport {
        private String fileName;
        private String className;
        private String methodName;
        private double confidence;
        private String evidence;
        @Builder.Default private List<String> dependencyPath = new ArrayList<>();
    }
}
