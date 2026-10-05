package com.apianalyzer.ingestion.application.service;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Service
public class OpenApiParserService {
    public static class ParsedApi {
        public String title;
        public String version;
        public List<EndpointInfo> endpoints = new ArrayList<>();
    }
    public static class EndpointInfo {
        public String method; public String path; public String operationId;
        public EndpointInfo(String m, String p, String o) { this.method = m; this.path = p; this.operationId = o; }
    }
    public ParsedApi parse(String content) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, null);
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            throw new IllegalArgumentException("Invalid OpenAPI spec: " + String.join(", ", result.getMessages()));
        }
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) throw new IllegalArgumentException("Failed to parse OpenAPI document");
        
        ParsedApi parsed = new ParsedApi();
        parsed.title = openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null ? openAPI.getInfo().getTitle() : "Untitled API";
        parsed.version = openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null ? openAPI.getInfo().getVersion() : "1.0.0";
        
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                addOperation(parsed, "GET", path, pathItem.getGet());
                addOperation(parsed, "POST", path, pathItem.getPost());
                addOperation(parsed, "PUT", path, pathItem.getPut());
                addOperation(parsed, "DELETE", path, pathItem.getDelete());
                addOperation(parsed, "PATCH", path, pathItem.getPatch());
            }
        }
        return parsed;
    }
    private void addOperation(ParsedApi parsed, String method, String path, Operation op) {
        if (op != null) parsed.endpoints.add(new EndpointInfo(method, path, op.getOperationId()));
    }
}
