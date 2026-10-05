package com.apianalyzer.analysis.application.service.graph;
import com.apianalyzer.analysis.domain.model.ast.AstModels.*;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ApiConsumerImpact;
import com.apianalyzer.analysis.domain.model.graph.*;
import com.apianalyzer.analysis.domain.model.graph.ImpactNode.NodeType;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class ImpactGraphBuilderService {
    
    public ImpactGraph buildGraph(List<ApiChange> changes, List<ApiConsumerImpact> directImpacts, List<ClassInfo> consumerAst) {
        ImpactGraph graph = new ImpactGraph();
        
        // 1. Add API Changes & Endpoints
        for (ApiChange change : changes) {
            String changeId = "change_" + change.hashCode();
            String endpointId = "ep_" + change.getMethod() + "_" + change.getPath();
            
            graph.addNode(ImpactNode.builder().id(changeId).type(NodeType.API_CHANGE)
                .label(change.getType().name()).payload(change).build());
                
            graph.addNode(ImpactNode.builder().id(endpointId).type(NodeType.ENDPOINT)
                .label(change.getMethod() + " " + change.getPath()).build());
                
            graph.addEdge(ImpactEdge.builder().sourceId(changeId).targetId(endpointId)
                .relationship("AFFECTS_ENDPOINT").confidence(1.0).build());
        }
        
        // 2. Map Direct Impacts (Endpoint -> Consumer Method/Class)
        for (ApiConsumerImpact impact : directImpacts) {
            String endpointId = "ep_" + impact.getRelatedChange().getMethod() + "_" + impact.getRelatedChange().getPath();
            String classId = "class_" + impact.getClassName();
            
            graph.addNode(ImpactNode.builder().id(classId).type(NodeType.CLASS).label(impact.getClassName()).build());
            
            if (impact.getMethodName() != null && !impact.getMethodName().contains("Class-level")) {
                String methodId = "method_" + impact.getClassName() + "." + impact.getMethodName();
                graph.addNode(ImpactNode.builder().id(methodId).type(NodeType.METHOD).label(impact.getMethodName()).build());
                
                // Link Endpoint -> Method -> Class
                graph.addEdge(ImpactEdge.builder().sourceId(endpointId).targetId(methodId)
                    .relationship("CONSUMED_BY").confidence(impact.getConfidence()).build());
                graph.addEdge(ImpactEdge.builder().sourceId(methodId).targetId(classId)
                    .relationship("BELONGS_TO").confidence(1.0).build());
            } else {
                // Link Endpoint -> Class
                graph.addEdge(ImpactEdge.builder().sourceId(endpointId).targetId(classId)
                    .relationship("CONSUMED_BY").confidence(impact.getConfidence()).build());
            }
        }
        
        // 3. Transitive Dependency Traversal (Method -> Method -> Service -> Repo)
        boolean addedTransitive = true;
        
        // Simple fixed-point traversal
        while (addedTransitive) {
            addedTransitive = false;
            for (ClassInfo clazz : consumerAst) {
                for (MethodInfo method : clazz.getMethods()) {
                    String callerMethodId = "method_" + clazz.getClassName() + "." + method.getName();
                    
                    for (MethodCallInfo call : method.getMethodCalls()) {
                        // Very naive resolution assuming scope roughly maps to class variable types 
                        // In a real engine, we resolve AST symbols. Here we do an approximation.
                        String calleeClass = resolveClassFromScope(call.getScope(), clazz); 
                        String calleeMethodId = "method_" + calleeClass + "." + call.getName();
                        
                        // If the callee is ALREADY in our graph (meaning it was impacted), the caller is also impacted!
                        if (graph.getNodes().containsKey(calleeMethodId) && !graph.getNodes().containsKey(callerMethodId)) {
                            graph.addNode(ImpactNode.builder().id(callerMethodId).type(NodeType.METHOD).label(method.getName()).build());
                            graph.addEdge(ImpactEdge.builder().sourceId(calleeMethodId).targetId(callerMethodId)
                                .relationship("CALLED_BY").confidence(0.75).build()); // Confidence decays over transitive links
                            
                            String callerClassId = "class_" + clazz.getClassName();
                            graph.addNode(ImpactNode.builder().id(callerClassId).type(NodeType.CLASS)
                                .label(clazz.getClassName()).build());
                            graph.addEdge(ImpactEdge.builder().sourceId(callerMethodId).targetId(callerClassId)
                                .relationship("BELONGS_TO").confidence(1.0).build());
                                
                            addedTransitive = true;
                        }
                    }
                }
            }
        }
        return graph;
    }
    
    private String resolveClassFromScope(String scope, ClassInfo currentClass) {
        // Simplified lookup (e.g. scope "userService" -> class "UserService")
        if (scope.equals("this")) return currentClass.getClassName();
        return scope.substring(0, 1).toUpperCase() + scope.substring(1);
    }
}
