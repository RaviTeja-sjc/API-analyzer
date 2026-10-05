package com.apianalyzer.analysis.domain.model.diff;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImpactClassification {
    public enum Level { LOW, MEDIUM, HIGH, CRITICAL }
    
    private Level level;
    private int score; // 0-100
    private double confidence; // 0.0 - 1.0
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    
    public void addReason(String reason) {
        this.reasons.add(reason);
    }
}
