package com.apianalyzer.analysis.application.service.graph;

import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodCallInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyGraphBuilderServiceTest {

    private final DependencyGraphBuilderService service = new DependencyGraphBuilderService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void testBuildGraph_WithSimpleControllerServiceRepository() throws Exception {
        ClassInfo repository = ClassInfo.builder()
                .className("UserRepository")
                .packageName("com.example")
                .annotations(List.of("Repository"))
                .build();

        ClassInfo serviceClass = ClassInfo.builder()
                .className("UserService")
                .packageName("com.example")
                .annotations(List.of("Service"))
                .imports(List.of("com.example.UserRepository"))
                .typeReferences(List.of("UserRepository"))
                .methods(List.of(
                        MethodInfo.builder()
                                .name("getUser")
                                .methodCalls(List.of(
                                        MethodCallInfo.builder().scope("userRepository").name("findById").build()
                                ))
                                .build()
                ))
                .build();

        ClassInfo controller = ClassInfo.builder()
                .className("UserController")
                .packageName("com.example")
                .annotations(List.of("RestController"))
                .imports(List.of("com.example.UserService"))
                .typeReferences(List.of("UserService"))
                .methods(List.of(
                        MethodInfo.builder()
                                .name("getUser")
                                .methodCalls(List.of(
                                        MethodCallInfo.builder().scope("userService").name("getUser").build()
                                ))
                                .build()
                ))
                .build();

        String json = service.buildGraph(Arrays.asList(controller, serviceClass, repository), Collections.emptyList());
        
        Map<String, Object> result = mapper.readValue(json, Map.class);
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.get("nodes");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");

        assertEquals(3, nodes.size());
        
        // Assert Node Types
        assertTrue(nodes.stream().anyMatch(n -> n.get("id").equals("UserController") && n.get("className").equals("node-endpoint")));
        assertTrue(nodes.stream().anyMatch(n -> n.get("id").equals("UserService") && n.get("className").equals("node-consumer")));
        assertTrue(nodes.stream().anyMatch(n -> n.get("id").equals("UserRepository") && n.get("className").equals("node-transitive")));
        
        // Assert Edges (Controller -> Service, Service -> Repository)
        // Controller imports, references, and calls UserService
        assertTrue(edges.stream().anyMatch(e -> e.get("source").equals("UserController") && e.get("target").equals("UserService") && e.get("type").equals("IMPORTS")));
        assertTrue(edges.stream().anyMatch(e -> e.get("source").equals("UserController") && e.get("target").equals("UserService") && e.get("type").equals("REFERENCES")));
        assertTrue(edges.stream().anyMatch(e -> e.get("source").equals("UserController") && e.get("target").equals("UserService") && e.get("type").equals("CALLS")));

        // Service imports, references, and calls UserRepository
        assertTrue(edges.stream().anyMatch(e -> e.get("source").equals("UserService") && e.get("target").equals("UserRepository") && e.get("type").equals("IMPORTS")));
        assertTrue(edges.stream().anyMatch(e -> e.get("source").equals("UserService") && e.get("target").equals("UserRepository") && e.get("type").equals("REFERENCES")));
        assertTrue(edges.stream().anyMatch(e -> e.get("source").equals("UserService") && e.get("target").equals("UserRepository") && e.get("type").equals("CALLS")));
    }
    
    @Test
    void testBuildGraph_Determinism() throws Exception {
        ClassInfo c1 = ClassInfo.builder().className("ClassA").imports(List.of("com.example.ClassB")).build();
        ClassInfo c2 = ClassInfo.builder().className("ClassB").build();
        
        String run1 = service.buildGraph(Arrays.asList(c1, c2), Collections.emptyList());
        String run2 = service.buildGraph(Arrays.asList(c1, c2), Collections.emptyList());
        
        assertEquals(run1, run2, "Graph generation should be deterministic");
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void testBuildGraph_NoFalseEdges() throws Exception {
        ClassInfo c1 = ClassInfo.builder().className("ClassA").build();
        ClassInfo c2 = ClassInfo.builder().className("ClassB").build();
        
        String json = service.buildGraph(Arrays.asList(c1, c2), Collections.emptyList());
        Map<String, Object> result = mapper.readValue(json, Map.class);
        List<Map<String, Object>> edges = (List<Map<String, Object>>) result.get("edges");
        
        assertTrue(edges.isEmpty(), "Unrelated classes should have no edges");
    }
}
