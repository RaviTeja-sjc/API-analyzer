package com.apianalyzer.analysis.application.service.graph;

import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodCallInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodInfo;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class DependencyGraphBuilderService {

    public String buildGraph(List<ClassInfo> classes, List<Map<String, Object>> files) {
        return buildImpactGraph(classes, files, Collections.emptyList());
    }

    public String buildImpactGraph(List<ClassInfo> classes, List<Map<String, Object>> files, List<ApiChange> apiChanges) {
        try {
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();

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

                nodeMap.put(c.getClassName(), node);
            }

            int edgeId = 1;

            // 1. If we have API changes, create Endpoint nodes and connect them to matching controllers
            if (apiChanges != null) {
                for (ApiChange change : apiChanges) {
                    String epId = "ep_" + (change.getMethod() != null ? change.getMethod().toUpperCase() : "ANY") + "_" + sanitizePath(change.getPath());
                    String epLabel = (change.getMethod() != null ? change.getMethod().toUpperCase() : "") + " " + change.getPath();

                    boolean isBreaking = change.getSeverity() != null && "BREAKING".equalsIgnoreCase(change.getSeverity().name());
                    String epClass = isBreaking ? "node-change" : "node-endpoint";

                    Map<String, Object> epNode = new HashMap<>();
                    epNode.put("id", epId);
                    epNode.put("className", epClass);

                    Map<String, Object> data = new HashMap<>();
                    data.put("label", epLabel);
                    data.put("type", epClass);
                    data.put("filePath", change.getPath());
                    data.put("severity", change.getSeverity() != null ? change.getSeverity().name() : "INFO");
                    epNode.put("data", data);

                    nodeMap.put(epId, epNode);

                    // Find matching controllers
                    for (ClassInfo c : classes) {
                        if (c.isController() && isMatchingEndpoint(c, change)) {
                            Map<String, Object> edge = new HashMap<>();
                            edge.put("id", "e" + (edgeId++));
                            edge.put("source", epId);
                            edge.put("target", c.getClassName());
                            edge.put("label", "HANDLES");
                            edge.put("type", "HANDLES");
                            edge.put("confidence", 1.0);
                            edge.put("evidence", c.getClassName() + " handles API endpoint " + epLabel);
                            edges.add(edge);
                        }
                    }
                }
            }

            // 2. Class-to-Class relationships
            for (ClassInfo c : classes) {
                // Relationship A: IMPORTS
                for (String imp : c.getImports()) {
                    String simpleName = imp.substring(imp.lastIndexOf('.') + 1);
                    if (classMap.containsKey(simpleName) && !simpleName.equals(c.getClassName())) {
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
                    if (classMap.containsKey(ref) && !ref.equals(c.getClassName())) {
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
                        if (scope != null && !scope.equals("this")) {
                            if (classMap.containsKey(scope) && !scope.equals(c.getClassName())) {
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
                                    if ((scope.equalsIgnoreCase(known) || (scope + "Impl").equalsIgnoreCase(known)) && !known.equals(c.getClassName())) {
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

            // 3. Fallback for non-Java repos
            if (classes.isEmpty() && files != null && !files.isEmpty()) {
                for (Map<String, Object> file : files) {
                    String path = (String) file.get("path");
                    if (path == null) continue;
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", path);
                    node.put("className", "node-consumer");
                    node.put("data", Map.of("label", path.length() > 30 ? path.substring(path.length() - 30) : path));
                    nodeMap.put(path, node);

                    int lastSlash = path.lastIndexOf("/");
                    if (lastSlash > 0) {
                        String dir = path.substring(0, lastSlash);
                        if (!nodeMap.containsKey(dir)) {
                            Map<String, Object> dirNode = new HashMap<>();
                            dirNode.put("id", dir);
                            dirNode.put("className", "node-default");
                            dirNode.put("data", Map.of("label", dir));
                            nodeMap.put(dir, dirNode);
                        }
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("id", "e" + (edgeId++));
                        edge.put("source", dir);
                        edge.put("target", path);
                        edges.add(edge);
                    }
                }
            }

            // Deduplicate edges & verify both endpoints exist in nodeMap
            List<Map<String, Object>> uniqueEdges = new ArrayList<>();
            Set<String> seenEdges = new HashSet<>();
            for (Map<String, Object> e : edges) {
                String source = (String) e.get("source");
                String target = (String) e.get("target");
                if (nodeMap.containsKey(source) && nodeMap.containsKey(target) && !source.equals(target)) {
                    String key = source + "->" + target + ":" + e.get("type");
                    if (seenEdges.add(key)) {
                        uniqueEdges.add(e);
                    }
                }
            }

            nodes.addAll(nodeMap.values());
            Map<String, Object> graph = Map.of("nodes", nodes, "edges", uniqueEdges);
            return new ObjectMapper().writeValueAsString(graph);

        } catch (Exception e) {
            log.error("Failed to build dependency graph: {}", e.getMessage(), e);
            return "{\"nodes\":[],\"edges\":[]}";
        }
    }

    private boolean isMatchingEndpoint(ClassInfo controller, ApiChange change) {
        if (change.getPath() == null) return false;
        String changeMethod = change.getMethod() != null ? change.getMethod().toUpperCase() : "";
        String changePath = normalizePath(change.getPath());

        for (MethodInfo m : controller.getMethods()) {
            if (m.getFullEndpointPath() != null && !m.getFullEndpointPath().isEmpty()) {
                String mPath = normalizePath(m.getFullEndpointPath());
                boolean methodMatch = changeMethod.isEmpty() || m.getHttpMethod() == null || changeMethod.equalsIgnoreCase(m.getHttpMethod());
                if (methodMatch && pathsMatch(changePath, mPath)) {
                    return true;
                }
            }
        }

        // Check if class base path matches root of change path
        if (controller.getBasePath() != null && !controller.getBasePath().isEmpty()) {
            String cBasePath = normalizePath(controller.getBasePath());
            if (changePath.startsWith(cBasePath)) {
                return true;
            }
        }
        return false;
    }

    private String sanitizePath(String path) {
        if (path == null) return "root";
        return path.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String normalizePath(String path) {
        if (path == null) return "/";
        String p = path.trim().toLowerCase();
        if (!p.startsWith("/")) p = "/" + p;
        if (p.endsWith("/") && p.length() > 1) p = p.substring(0, p.length() - 1);
        // Replace path variable templates e.g. {id} or {userId} with {*}
        p = p.replaceAll("\\{[^}]+\\}", "*");
        return p;
    }

    private boolean pathsMatch(String p1, String p2) {
        if (p1.equals(p2)) return true;
        String[] s1 = p1.split("/");
        String[] s2 = p2.split("/");
        if (s1.length != s2.length) return false;
        for (int i = 0; i < s1.length; i++) {
            if (!s1[i].equals(s2[i]) && !s1[i].equals("*") && !s2[i].equals("*")) {
                return false;
            }
        }
        return true;
    }
}
