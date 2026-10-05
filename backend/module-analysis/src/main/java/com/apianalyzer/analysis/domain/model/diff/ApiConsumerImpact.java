package com.apianalyzer.analysis.domain.model.diff;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiConsumerImpact {
    private String fileName;
    private String className;
    private String methodName;
    private String evidence;
    private ApiChange relatedChange;
    private double confidence; // 0.0 to 1.0
}
