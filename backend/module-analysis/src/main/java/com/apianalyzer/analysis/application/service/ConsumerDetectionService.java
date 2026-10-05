package com.apianalyzer.analysis.application.service;
import com.apianalyzer.analysis.application.service.ast.JavaAstParserService;
import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodCallInfo;
import com.apianalyzer.analysis.domain.model.ast.AstModels.MethodInfo;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import com.apianalyzer.analysis.domain.model.diff.ApiConsumerImpact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class ConsumerDetectionService {
    private final JavaAstParserService astParser;
    
    public List<ApiConsumerImpact> detectImpacts(String fileName, String sourceCode, List<ApiChange> changes) {
        List<ApiConsumerImpact> impacts = new ArrayList<>();
        List<ClassInfo> classes = astParser.parseSourceCode(sourceCode);
        
        for (ClassInfo clazz : classes) {
            for (ApiChange change : changes) {
                // 1. DTO / Property Level Match (Low/Med Confidence)
                String propertyName = extractPropertyName(change);
                if (propertyName != null && clazz.getTypeReferences().stream().anyMatch(t -> t.contains(propertyName) || t.equalsIgnoreCase(propertyName))) {
                    impacts.add(buildImpact(fileName, clazz.getClassName(), "Class-level Field/Type", 
                        "Class references type or property potentially related to API change: " + propertyName, change, 0.4));
                }
                
                // 2. Feign Client Match (High Confidence)
                if (clazz.isApiClient() && clazz.getAnnotations().stream().anyMatch(a -> a.contains(change.getPath()))) {
                    impacts.add(buildImpact(fileName, clazz.getClassName(), "Class-level FeignClient", 
                        "Feign client maps directly to changed endpoint path: " + change.getPath(), change, 0.95));
                }
                
                // 3. Method Level Scrutiny
                for (MethodInfo method : clazz.getMethods()) {
                    // Check Annotations (@GetMapping, etc) mapping to path
                    if (method.getAnnotations().stream().anyMatch(a -> a.contains(change.getPath()))) {
                        impacts.add(buildImpact(fileName, clazz.getClassName(), method.getName(), 
                            "Method annotation references changed endpoint path", change, 0.9));
                    }
                    
                    // Check REST Client Method Calls (RestTemplate, WebClient)
                    for (MethodCallInfo call : method.getMethodCalls()) {
                        if (call.getArguments().stream().anyMatch(arg -> arg.contains(change.getPath()))) {
                            impacts.add(buildImpact(fileName, clazz.getClassName(), method.getName(), 
                                "REST client method call matches endpoint path: " + call.getScope() + "." + call.getName(), change, 0.85));
                        }
                    }
                }
            }
        }
        return impacts;
    }
    
    private String extractPropertyName(ApiChange change) {
        if (change.getDescription() != null && change.getDescription().contains("property")) {
            String[] parts = change.getDescription().split(" ");
            return parts[parts.length - 2]; // Approximation based on typical diff string format
        }
        return null;
    }
    
    private ApiConsumerImpact buildImpact(String file, String clazz, String method, String evidence, ApiChange change, double confidence) {
        return ApiConsumerImpact.builder().fileName(file).className(clazz).methodName(method)
            .evidence(evidence).relatedChange(change).confidence(confidence).build();
    }
}
