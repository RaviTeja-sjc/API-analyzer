package com.apianalyzer.analysis.domain.model.ast;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
public class AstModels {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassInfo {
        private String packageName;
        private String className;
        private boolean isInterface;
        @Builder.Default private List<String> annotations = new ArrayList<>();
        @Builder.Default private List<MethodInfo> methods = new ArrayList<>();
        @Builder.Default private List<String> typeReferences = new ArrayList<>();
        
        public boolean isController() { return annotations.stream().anyMatch(a -> a.contains("RestController") || a.contains("Controller")); }
        public boolean isService() { return annotations.contains("Service"); }
        public boolean isRepository() { return annotations.contains("Repository"); }
        public boolean isApiClient() { return annotations.contains("FeignClient"); }
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MethodInfo {
        private String name;
        private String returnType;
        @Builder.Default private List<String> annotations = new ArrayList<>();
        @Builder.Default private List<MethodCallInfo> methodCalls = new ArrayList<>();
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MethodCallInfo {
        private String scope;
        private String name;
        @Builder.Default private List<String> arguments = new ArrayList<>();
    }
}
