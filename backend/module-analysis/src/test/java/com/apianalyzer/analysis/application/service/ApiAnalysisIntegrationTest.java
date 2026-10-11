package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.application.service.rules.DiffRule;
import com.apianalyzer.analysis.application.service.rules.EndpointAddedRule;
import com.apianalyzer.analysis.application.service.rules.EndpointRemovedRule;
import com.apianalyzer.analysis.application.service.rules.ParameterRule;
import com.apianalyzer.analysis.domain.model.NormalizedApiModel;
import com.apianalyzer.analysis.domain.model.diff.ApiChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.apianalyzer.analysis.application.service.rules.RequestBodyRule;
import com.apianalyzer.analysis.application.service.rules.ResponseRule;
import com.apianalyzer.analysis.domain.model.diff.ChangeSeverity;
import com.apianalyzer.analysis.domain.model.diff.ChangeType;
import com.apianalyzer.analysis.domain.model.diff.ImpactClassification;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApiAnalysisIntegrationTest {

    private OpenApiNormalizer normalizer;
    private ApiDiffEngine diffEngine;

    @BeforeEach
    void setUp() {
        normalizer = new OpenApiNormalizer();
        List<DiffRule> rules = Arrays.asList(
            new EndpointAddedRule(),
            new EndpointRemovedRule(),
            new ParameterRule(),
            new RequestBodyRule(),
            new ResponseRule()
        );
        diffEngine = new ApiDiffEngine(rules);
    }

    @Test
    void test1_NoFalseBreakingChange() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users/{id}:
                get:
                  operationId: getUser
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema:
                        type: string
                  responses:
                    '200':
                      description: OK
            """;
            
        String headSpec = baseSpec;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertTrue(changes.isEmpty(), "Expected no changes, found: " + changes.size());
    }

    @Test
    void test2_EndpointRemoved() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users/{id}:
                get:
                  operationId: getUser
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema:
                        type: string
                  responses:
                    '200':
                      description: OK
            """;
            
        String headSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths: {}
            """;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertEquals(1, changes.size());
        assertEquals("ENDPOINT_REMOVED", changes.get(0).getType().name());
        assertEquals("/users/{id}", changes.get(0).getPath());
    }

    @Test
    void test3_RequiredParameterRemoved() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users/{id}:
                get:
                  operationId: getUser
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema:
                        type: string
                    - name: page
                      in: query
                      required: true
                      schema:
                        type: integer
                  responses:
                    '200':
                      description: OK
            """;
            
        String headSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users/{id}:
                get:
                  operationId: getUser
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema:
                        type: string
                  responses:
                    '200':
                      description: OK
            """;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertEquals(1, changes.size());
        assertTrue(changes.get(0).getDescription().contains("page"));
        assertEquals("PARAMETER_REMOVED", changes.get(0).getType().name());
    }

    @Test
    void test4_NewlyRequiredParameterAddedIsBreaking() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  parameters: []
                  responses:
                    '200':
                      description: OK
            """;

        String headSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  parameters:
                    - name: organizationId
                      in: query
                      required: true
                      schema:
                        type: string
                  responses:
                    '200':
                      description: OK
            """;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertFalse(changes.isEmpty());
        ApiChange change = changes.stream()
            .filter(c -> c.getType() == ChangeType.PARAMETER_ADDED && c.getSeverity() == ChangeSeverity.BREAKING)
            .findFirst()
            .orElseThrow();
        assertEquals(ChangeSeverity.BREAKING, change.getSeverity());
        assertNotNull(change.getImpact());
        assertTrue(change.getImpact().getLevel() == ImpactClassification.Level.HIGH || change.getImpact().getLevel() == ImpactClassification.Level.CRITICAL);
    }

    @Test
    void test5_NewlyRequiredRequestBodyIsBreaking() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /items:
                post:
                  requestBody:
                    required: false
                    content:
                      application/json:
                        schema:
                          type: object
                  responses:
                    '201':
                      description: Created
            """;

        String headSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /items:
                post:
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          type: object
                  responses:
                    '201':
                      description: Created
            """;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertFalse(changes.isEmpty());
        ApiChange change = changes.stream()
            .filter(c -> c.getType() == ChangeType.REQUEST_BODY_CHANGED)
            .findFirst()
            .orElseThrow();
        assertEquals(ChangeSeverity.BREAKING, change.getSeverity());
    }

    @Test
    void test6_RemovedResponseStatusIsBreaking() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /orders:
                get:
                  responses:
                    '200':
                      description: OK
                    '204':
                      description: No Content
            """;

        String headSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /orders:
                get:
                  responses:
                    '200':
                      description: OK
            """;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertFalse(changes.isEmpty());
        ApiChange change = changes.stream()
            .filter(c -> c.getType() == ChangeType.RESPONSE_REMOVED)
            .findFirst()
            .orElseThrow();
        assertEquals(ChangeSeverity.BREAKING, change.getSeverity());
        assertEquals("HTTP 204", change.getOldValue());
    }

    @Test
    void test7_RemovedEnumInResponseIsBreaking() {
        String baseSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /status:
                get:
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema:
                            type: string
                            enum: [ACTIVE, PENDING, ARCHIVED]
            """;

        String headSpec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /status:
                get:
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema:
                            type: string
                            enum: [ACTIVE, PENDING]
            """;

        NormalizedApiModel.Api baseApi = normalizer.normalize(baseSpec);
        NormalizedApiModel.Api headApi = normalizer.normalize(headSpec);

        List<ApiChange> changes = diffEngine.compare(baseApi, headApi);
        assertFalse(changes.isEmpty());
        ApiChange change = changes.stream()
            .filter(c -> c.getType() == ChangeType.ENUM_VALUE_REMOVED)
            .findFirst()
            .orElseThrow();
        assertEquals(ChangeSeverity.BREAKING, change.getSeverity());
        assertEquals("ARCHIVED", change.getOldValue());
    }
}
