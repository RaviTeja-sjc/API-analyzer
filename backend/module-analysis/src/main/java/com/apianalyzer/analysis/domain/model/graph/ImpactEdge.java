package com.apianalyzer.analysis.domain.model.graph;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImpactEdge {
    private String sourceId;
    private String targetId;
    private String relationship;
    private double confidence;
}
