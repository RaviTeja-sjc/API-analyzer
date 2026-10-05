package com.apianalyzer.analysis.domain.model.graph;
import lombok.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Data
public class ImpactGraph {
    private final Map<String, ImpactNode> nodes = new HashMap<>();
    private final List<ImpactEdge> edges = new ArrayList<>();
    
    public void addNode(ImpactNode node) { nodes.putIfAbsent(node.getId(), node); }
    public void addEdge(ImpactEdge edge) { edges.add(edge); }
    public ImpactNode getNode(String id) { return nodes.get(id); }
}
