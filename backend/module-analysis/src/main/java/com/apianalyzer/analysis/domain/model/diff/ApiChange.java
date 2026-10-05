package com.apianalyzer.analysis.domain.model.diff;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiChange {
    private ChangeType type;
    private ChangeSeverity severity;
    private String description;
    private String path;
    private String method;
    private String oldValue;
    private String newValue;
    private ImpactClassification impact; // Added impact classification
}
