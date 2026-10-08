package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.domain.model.NormalizedApiModel;
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
public class OpenApiNormalizer {

    public NormalizedApiModel.Api normalize(String content) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, null);
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            throw new IllegalArgumentException("Invalid OpenAPI spec: " + String.join(", ", result.getMessages()));
        }
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Failed to parse OpenAPI document");
        }

        NormalizedApiModel.Api api = new NormalizedApiModel.Api();
        api.setTitle(openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null ? openAPI.getInfo().getTitle() : "Untitled API");
        api.setVersion(openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null ? openAPI.getInfo().getVersion() : "1.0.0");

        List<NormalizedApiModel.Endpoint> endpoints = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                
                addEndpoint(endpoints, "GET", path, pathItem.getGet());
                addEndpoint(endpoints, "POST", path, pathItem.getPost());
                addEndpoint(endpoints, "PUT", path, pathItem.getPut());
                addEndpoint(endpoints, "DELETE", path, pathItem.getDelete());
                addEndpoint(endpoints, "PATCH", path, pathItem.getPatch());
            }
        }
        api.setEndpoints(endpoints);
        return api;
    }

    private void addEndpoint(List<NormalizedApiModel.Endpoint> endpoints, String method, String path, Operation op) {
        if (op != null) {
            NormalizedApiModel.Endpoint endpoint = new NormalizedApiModel.Endpoint();
            endpoint.setMethod(method);
            endpoint.setPath(path);
            endpoint.setOperationId(op.getOperationId());
            
            List<NormalizedApiModel.Parameter> parameters = new ArrayList<>();
            if (op.getParameters() != null) {
                for (io.swagger.v3.oas.models.parameters.Parameter p : op.getParameters()) {
                    NormalizedApiModel.Parameter param = new NormalizedApiModel.Parameter();
                    param.setName(p.getName());
                    param.setIn(p.getIn());
                    param.setRequired(Boolean.TRUE.equals(p.getRequired()));
                    
                    if (p.getSchema() != null) {
                        NormalizedApiModel.Schema schema = new NormalizedApiModel.Schema();
                        schema.setType(p.getSchema().getType());
                        param.setSchema(schema);
                    }
                    parameters.add(param);
                }
            }
            endpoint.setParameters(parameters);
            endpoints.add(endpoint);
        }
    }
}
