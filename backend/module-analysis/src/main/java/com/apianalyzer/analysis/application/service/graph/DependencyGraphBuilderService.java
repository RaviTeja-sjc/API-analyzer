package com.apianalyzer.analysis.application.service.graph;

import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodCallInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DependencyGraphBuilderService {

    public String buildGraph(List<ClassInfo> classes, List<Map<String, Object>> files) {
        try {
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            
            Map<String, ClassInfo> classMap = new HashMap<>();
            for (ClassInfo c : classes) {
                classMap.put(c.getClassName(), c);
                Map<String, Object> node = new HashMap<>();
                node.put("id", c.getClassName());
                
                String nodeType = c.isController() ? "node-endpoint" : (c.isRepository() ? "node-transitive" : (c.isService() ? "node-consumer" : "node-default"));
                node.put("className", nodeType);
                
                Map<String, Object> data = new HashMap<>();
                data.put("label", c.getClassName());
                data.put("type", nodeType);
                data.put("filePath", c.getFilePath());
                data.put("package", c.getPackageName());
                node.put("data", data);
                
                nodes.add(node);
            }
            
            int edgeId = 1;
            for (ClassInfo c : classes) {
                // Relationship A: IMPORTS
                for (String imp : c.getImports()) {
                    String simpleName = imp.substring(imp.lastIndexOf('.') + 1);
                    if (classMap.containsKey(simpleName)) {
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("id", "e" + (edgeId++));
                        edge.put("source", c.getClassName());
                        edge.put("target", simpleName);
                        edge.put("label", "IMPORTS");
                        edge.put("type", "IMPORTS");
                        edge.put("confidence", 1.0);
                        edge.put("evidence", c.getClassName() + " imports " + imp);
                        edges.add(edge);
                    }
                }
                
                // Relationship B: REFERENCES
                for (String ref : c.getTypeReferences()) {
                    if (classMap.containsKey(ref)) {
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("id", "e" + (edgeId++));
                        edge.put("source", c.getClassName());
                        edge.put("target", ref);
                        edge.put("label", "REFERENCES");
                        edge.put("type", "REFERENCES");
                        edge.put("confidence", 0.9);
                        edge.put("evidence", c.getClassName() + " references type " + ref);
                        edges.add(edge);
                    }
                }
                
                // Relationship C: CALLS
                for (MethodInfo m : c.getMethods()) {
                    for (MethodCallInfo mc : m.getMethodCalls()) {
                        String scope = mc.getScope();
                        if (scope != null) {
                            if (classMap.containsKey(scope)) {
                                Map<String, Object> edge = new HashMap<>();
                                edge.put("id", "e" + (edgeId++));
                                edge.put("source", c.getClassName());
                                edge.put("target", scope);
                                edge.put("label", "CALLS");
                                edge.put("type", "CALLS");
                                edge.put("confidence", 0.8);
                                edge.put("evidence", c.getClassName() + "." + m.getName() + " calls " + scope + "." + mc.getName());
                                edges.add(edge);
                            } else {
                                for (String known : classMap.keySet()) {
                                    if (scope.equalsIgnoreCase(known) || (scope + "Impl").equalsIgnoreCase(known)) {
                                        Map<String, Object> edge = new HashMap<>();
                                        edge.put("id", "e" + (edgeId++));
                                        edge.put("source", c.getClassName());
                                        edge.put("target", known);
                                        edge.put("label", "CALLS");
                                        edge.put("type", "CALLS");
                                        edge.put("confidence", 0.7);
                                        edge.put("evidence", c.getClassName() + "." + m.getName() + " calls variable '" + scope + "' mapped to " + known);
                                        edges.add(edge);
                                        break; 
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Deduplicate edges
            List<Map<String, Object>> uniqueEdges = new ArrayList<>();
            java.util.Set<String> seenEdges = new java.util.HashSet<>();
            for (Map<String, Object> e : edges) {
                String key = e.get("source") + "->" + e.get("target") + ":" + e.get("type");
                if (seenEdges.add(key)) {
                    uniqueEdges.add(e);
                }
            }
            
            if (classes.isEmpty() && !files.isEmpty()) {
                for (Map<String, Object> file : files) {
                    String path = (String) file.get("path");
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", path);
                    node.put("className", "node-consumer");
                    node.put("data", Map.of("label", path.length() > 30 ? path.substring(path.length() - 30) : path));
                    nodes.add(node);
                    
                    int lastSlash = path.lastIndexOf("/");
                    if (lastSlash > 0) {
                        String dir = path.substring(0, lastSlash);
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("id", "e" + (edgeId++));
                        edge.put("source", dir); 
                        edge.put("target", path);
                        uniqueEdges.add(edge);
                    }
                }
            }
            
            Map<String, Object> graph = Map.of("nodes", nodes, "edges", uniqueEdges);
            return new ObjectMapper().writeValueAsString(graph);
            
        } catch (Exception e) {
            return "{}";
        }
    }
}
