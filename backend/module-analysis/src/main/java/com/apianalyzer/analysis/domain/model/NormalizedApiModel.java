package com.apianalyzer.analysis.domain.model;
import lombok.*;
import java.util.*;
public class NormalizedApiModel {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Api {
        private String title;
        private String version;
        @Builder.Default private List<Endpoint> endpoints = new ArrayList<>();
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Endpoint {
        private String method; // GET, POST, etc.
        private String path;
        private String operationId;
        @Builder.Default private List<Parameter> parameters = new ArrayList<>();
        private RequestBody requestBody;
        @Builder.Default private Map<String, Response> responses = new HashMap<>(); // Keyed by HTTP status
        @Builder.Default private List<Map<String, List<String>>> securityRequirements = new ArrayList<>();
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Parameter {
        private String name;
        private String in; // query, path, header, cookie
        private boolean required;
        private Schema schema;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RequestBody {
        private boolean required;
        @Builder.Default private Map<String, Schema> content = new HashMap<>(); // Keyed by media type
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String statusCode;
        private String description;
        @Builder.Default private Map<String, Schema> content = new HashMap<>(); // Keyed by media type
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Schema {
        private String type; // string, number, integer, boolean, array, object
        private String format;
        private boolean nullable;
        private String ref; // Preserved for debugging, though engine should resolve these
        @Builder.Default private List<String> enumValues = new ArrayList<>();
        @Builder.Default private List<String> requiredFields = new ArrayList<>();
        @Builder.Default private Map<String, Schema> properties = new HashMap<>();
        private Schema items; // For array types
        private Schema additionalProperties; // For maps/dictionaries
        
        // Polymorphism support
        @Builder.Default private List<Schema> allOf = new ArrayList<>();
        @Builder.Default private List<Schema> oneOf = new ArrayList<>();
        @Builder.Default private List<Schema> anyOf = new ArrayList<>();
    }
}
