package com.apianalyzer.analysis.domain.model.ast;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

public class AstModels {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassInfo {
        private String packageName;
        private String className;
        private String filePath; // For source evidence
        private boolean isInterface;
        private String basePath; // e.g. from @RequestMapping("/api/v1")
        private Integer startLine;
        private Integer endLine;

        @Builder.Default private List<String> imports = new ArrayList<>();
        @Builder.Default private List<String> annotations = new ArrayList<>();
        @Builder.Default private List<MethodInfo> methods = new ArrayList<>();
        @Builder.Default private List<String> typeReferences = new ArrayList<>();

        public boolean isController() {
            return annotations.stream().anyMatch(a -> a.contains("RestController") || a.contains("Controller"));
        }
        public boolean isService() {
            return annotations.stream().anyMatch(a -> a.contains("Service"));
        }
        public boolean isRepository() {
            return annotations.stream().anyMatch(a -> a.contains("Repository"));
        }
        public boolean isApiClient() {
            return annotations.stream().anyMatch(a -> a.contains("FeignClient"));
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MethodInfo {
        private String name;
        private String returnType;
        private String httpMethod; // GET, POST, PUT, DELETE, PATCH, etc.
        private String methodPath; // Mapping path e.g. "/users" or "/{id}"
        private String fullEndpointPath; // Combined class basePath + methodPath
        private Integer startLine;
        private Integer endLine;

        @Builder.Default private List<String> annotations = new ArrayList<>();
        @Builder.Default private List<MethodCallInfo> methodCalls = new ArrayList<>();
        @Builder.Default private List<String> parameters = new ArrayList<>();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MethodCallInfo {
        private String scope;
        private String name;
        @Builder.Default private List<String> arguments = new ArrayList<>();
    }
}
