package com.apianalyzer.analysis.domain.model.migration;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MigrationSuggestion {
    private String affectedFile;
    private String affectedSymbol;
    private String oldUsage;
    private String newUsage;
    private String explanation;
    private double confidence; // 0.0 to 1.0
    private boolean requiresManualReview;
}
