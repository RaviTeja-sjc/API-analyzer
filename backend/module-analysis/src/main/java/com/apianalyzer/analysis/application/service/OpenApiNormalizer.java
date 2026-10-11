package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.domain.model.NormalizedApiModel;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class OpenApiNormalizer {

    public NormalizedApiModel.Api normalize(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("OpenAPI specification content is empty.");
        }

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, null);
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            String msg = (result.getMessages() != null && !result.getMessages().isEmpty())
                    ? String.join("; ", result.getMessages())
                    : "Unrecognized or malformed OpenAPI document.";
            throw new IllegalArgumentException("Failed to parse OpenAPI document: " + msg);
        }

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            log.debug("Parser non-fatal notices for OpenAPI document: {}", result.getMessages());
        }

        NormalizedApiModel.Api api = new NormalizedApiModel.Api();
        api.setTitle(openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null ? openAPI.getInfo().getTitle() : "Untitled API");
        api.setVersion(openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null ? openAPI.getInfo().getVersion() : "1.0.0");

        List<NormalizedApiModel.Endpoint> endpoints = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                if (pathItem == null) continue;

                addEndpoint(endpoints, "GET", path, pathItem.getGet(), openAPI);
                addEndpoint(endpoints, "POST", path, pathItem.getPost(), openAPI);
                addEndpoint(endpoints, "PUT", path, pathItem.getPut(), openAPI);
                addEndpoint(endpoints, "DELETE", path, pathItem.getDelete(), openAPI);
                addEndpoint(endpoints, "PATCH", path, pathItem.getPatch(), openAPI);
                addEndpoint(endpoints, "HEAD", path, pathItem.getHead(), openAPI);
                addEndpoint(endpoints, "OPTIONS", path, pathItem.getOptions(), openAPI);
            }
        }
        api.setEndpoints(endpoints);
        return api;
    }

    private void addEndpoint(List<NormalizedApiModel.Endpoint> endpoints, String method, String path, Operation op, OpenAPI openAPI) {
        if (op == null) return;

        NormalizedApiModel.Endpoint endpoint = new NormalizedApiModel.Endpoint();
        endpoint.setMethod(method);
        endpoint.setPath(path);
        endpoint.setOperationId(op.getOperationId());

        // 1. Parameters
        List<NormalizedApiModel.Parameter> parameters = new ArrayList<>();
        if (op.getParameters() != null) {
            for (io.swagger.v3.oas.models.parameters.Parameter p : op.getParameters()) {
                if (p == null) continue;
                NormalizedApiModel.Parameter param = new NormalizedApiModel.Parameter();
                param.setName(p.getName());
                param.setIn(p.getIn() != null ? p.getIn().toLowerCase() : "query");
                param.setRequired(Boolean.TRUE.equals(p.getRequired()));

                if (p.getSchema() != null) {
                    param.setSchema(normalizeSchema(p.getSchema(), openAPI, new HashSet<>()));
                }
                parameters.add(param);
            }
        }
        endpoint.setParameters(parameters);

        // 2. Request Body
        if (op.getRequestBody() != null) {
            RequestBody rb = op.getRequestBody();
            // Handle request body $ref
            if (rb.get$ref() != null && openAPI.getComponents() != null && openAPI.getComponents().getRequestBodies() != null) {
                String refKey = extractRefName(rb.get$ref());
                if (openAPI.getComponents().getRequestBodies().containsKey(refKey)) {
                    rb = openAPI.getComponents().getRequestBodies().get(refKey);
                }
            }

            NormalizedApiModel.RequestBody normalizedRb = new NormalizedApiModel.RequestBody();
            normalizedRb.setRequired(Boolean.TRUE.equals(rb.getRequired()));

            Content content = rb.getContent();
            if (content != null) {
                for (Map.Entry<String, MediaType> mtEntry : content.entrySet()) {
                    String mediaType = mtEntry.getKey();
                    MediaType mt = mtEntry.getValue();
                    if (mt != null && mt.getSchema() != null) {
                        normalizedRb.getContent().put(mediaType, normalizeSchema(mt.getSchema(), openAPI, new HashSet<>()));
                    }
                }
            }
            endpoint.setRequestBody(normalizedRb);
        }

        // 3. Responses
        ApiResponses responses = op.getResponses();
        if (responses != null) {
            Map<String, NormalizedApiModel.Response> responseMap = new HashMap<>();
            for (Map.Entry<String, ApiResponse> rEntry : responses.entrySet()) {
                String statusCode = rEntry.getKey();
                ApiResponse resp = rEntry.getValue();
                if (resp == null) continue;

                // Handle response $ref
                if (resp.get$ref() != null && openAPI.getComponents() != null && openAPI.getComponents().getResponses() != null) {
                    String refKey = extractRefName(resp.get$ref());
                    if (openAPI.getComponents().getResponses().containsKey(refKey)) {
                        resp = openAPI.getComponents().getResponses().get(refKey);
                    }
                }

                NormalizedApiModel.Response normResp = new NormalizedApiModel.Response();
                normResp.setStatusCode(statusCode);
                normResp.setDescription(resp.getDescription());

                Content content = resp.getContent();
                if (content != null) {
                    for (Map.Entry<String, MediaType> mtEntry : content.entrySet()) {
                        String mediaType = mtEntry.getKey();
                        MediaType mt = mtEntry.getValue();
                        if (mt != null && mt.getSchema() != null) {
                            normResp.getContent().put(mediaType, normalizeSchema(mt.getSchema(), openAPI, new HashSet<>()));
                        }
                    }
                }
                responseMap.put(statusCode, normResp);
            }
            endpoint.setResponses(responseMap);
        }

        endpoints.add(endpoint);
    }

    private NormalizedApiModel.Schema normalizeSchema(io.swagger.v3.oas.models.media.Schema<?> rawSchema, OpenAPI openAPI, Set<String> visitedRefs) {
        if (rawSchema == null) return null;

        // Resolve $ref if present
        if (rawSchema.get$ref() != null) {
            String ref = rawSchema.get$ref();
            if (visitedRefs.contains(ref)) {
                // Avoid infinite recursion on cyclic definitions
                NormalizedApiModel.Schema cyclic = new NormalizedApiModel.Schema();
                cyclic.setType("object");
                cyclic.setRef(ref);
                return cyclic;
            }
            visitedRefs.add(ref);

            String refName = extractRefName(ref);
            if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null
                    && openAPI.getComponents().getSchemas().containsKey(refName)) {
                io.swagger.v3.oas.models.media.Schema<?> resolved = openAPI.getComponents().getSchemas().get(refName);
                NormalizedApiModel.Schema norm = normalizeSchema(resolved, openAPI, visitedRefs);
                if (norm != null) {
                    norm.setRef(ref);
                    return norm;
                }
            }
        }

        NormalizedApiModel.Schema norm = new NormalizedApiModel.Schema();
        norm.setType(rawSchema.getType() != null ? rawSchema.getType().toLowerCase() : "object");
        norm.setFormat(rawSchema.getFormat());
        norm.setNullable(Boolean.TRUE.equals(rawSchema.getNullable()));

        // Enum values
        if (rawSchema.getEnum() != null) {
            for (Object enumVal : rawSchema.getEnum()) {
                if (enumVal != null) norm.getEnumValues().add(enumVal.toString());
            }
        }

        // Required fields
        if (rawSchema.getRequired() != null) {
            norm.getRequiredFields().addAll(rawSchema.getRequired());
        }

        // Object properties
        if (rawSchema.getProperties() != null) {
            for (Map.Entry<String, io.swagger.v3.oas.models.media.Schema> propEntry : rawSchema.getProperties().entrySet()) {
                String propName = propEntry.getKey();
                io.swagger.v3.oas.models.media.Schema propSchema = propEntry.getValue();
                norm.getProperties().put(propName, normalizeSchema(propSchema, openAPI, new HashSet<>(visitedRefs)));
            }
        }

        // Array items
        if (rawSchema.getItems() != null) {
            norm.setItems(normalizeSchema(rawSchema.getItems(), openAPI, new HashSet<>(visitedRefs)));
        }

        return norm;
    }

    private String extractRefName(String ref) {
        if (ref == null) return "";
        int lastSlash = ref.lastIndexOf('/');
        return lastSlash >= 0 ? ref.substring(lastSlash + 1) : ref;
    }
}
