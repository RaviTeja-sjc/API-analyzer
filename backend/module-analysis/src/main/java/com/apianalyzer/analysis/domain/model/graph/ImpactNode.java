package com.apianalyzer.analysis.domain.model.graph;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ImpactNode {
    public enum NodeType { API_CHANGE, ENDPOINT, CLASS, METHOD }
    
    @EqualsAndHashCode.Include
    private String id;
    private NodeType type;
    private String label;
    private Object payload; // Can store ApiChange, ClassInfo, etc.
}
