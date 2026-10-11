package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ApiSpecDiscoveryService {

    @Getter
    public static class DiscoveredSpec {
        private final String filePath;
        private final String content;
        private final String format; // YAML or JSON
        private final String specVersion;

        public DiscoveredSpec(String filePath, String content) {
            this.filePath = filePath;
            this.content = content;
            this.format = (filePath.endsWith(".json")) ? "JSON" : "YAML";
            this.specVersion = detectVersion(content);
        }

        private static String detectVersion(String text) {
            if (text == null) return "Unknown";
            String lower = text.toLowerCase();
            if (lower.contains("openapi: 3.") || lower.contains("\"openapi\": \"3.") || lower.contains("\"openapi\":\"3.")) {
                return "OpenAPI 3.x";
            }
            if (lower.contains("swagger: \"2.") || lower.contains("swagger: '2.") || lower.contains("\"swagger\": \"2.") || lower.contains("\"swagger\":\"2.")) {
                return "Swagger 2.0";
            }
            return "OpenAPI Specification";
        }
    }

    @Getter
    public static class DiscoveryReport {
        private final Optional<DiscoveredSpec> primarySpec;
        private final List<String> examinedFiles;
        private final List<String> matchedFiles;
        private final List<String> skippedReasons;

        public DiscoveryReport(Optional<DiscoveredSpec> primarySpec, List<String> examinedFiles, List<String> matchedFiles, List<String> skippedReasons) {
            this.primarySpec = primarySpec;
            this.examinedFiles = examinedFiles;
            this.matchedFiles = matchedFiles;
            this.skippedReasons = skippedReasons;
        }
    }

    public Optional<DiscoveredSpec> discoverOpenApiSpec(RepositoryProvider provider, RepositorySnapshot snapshot) {
        DiscoveryReport report = discoverWithReport(provider, snapshot);
        return report.getPrimarySpec();
    }

    public DiscoveryReport discoverWithReport(RepositoryProvider provider, RepositorySnapshot snapshot) {
        if (snapshot == null || snapshot.getFiles() == null || snapshot.getFiles().isEmpty()) {
            return new DiscoveryReport(Optional.empty(), Collections.emptyList(), Collections.emptyList(), List.of("Snapshot contains no files."));
        }

        List<String> examined = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        // Priority 1: High-confidence standard names in common API folders
        List<String> highPriorityNames = List.of(
            "openapi.yaml", "openapi.yml", "openapi.json",
            "swagger.yaml", "swagger.yml", "swagger.json",
            "api/openapi.yaml", "api/openapi.yml", "api/openapi.json",
            "api/swagger.yaml", "api/swagger.yml", "api/swagger.json",
            "docs/openapi.yaml", "docs/openapi.yml", "docs/openapi.json",
            "docs/swagger.yaml", "docs/swagger.yml", "docs/swagger.json",
            "spec/openapi.yaml", "spec/openapi.yml", "spec/openapi.json",
            "specs/openapi.yaml", "specs/openapi.yml", "specs/openapi.json",
            "specs/swagger.yaml", "specs/swagger.yml", "specs/swagger.json",
            "contracts/openapi.yaml", "contracts/openapi.yml", "contracts/openapi.json"
        );

        List<RepositorySnapshot.RepositoryFile> candidates = new ArrayList<>();

        for (RepositorySnapshot.RepositoryFile file : snapshot.getFiles()) {
            if (!"blob".equals(file.getType())) continue;
            String path = file.getPath().toLowerCase();

            // Ignore vendor and build artifacts
            if (path.contains("node_modules/") || path.contains("target/") || path.contains("build/") || path.contains(".git/") || path.contains("dist/")) {
                continue;
            }

            boolean isHighPriority = highPriorityNames.stream().anyMatch(hp -> hp.equalsIgnoreCase(file.getPath()) || file.getPath().toLowerCase().endsWith("/" + hp));
            boolean isExtensionMatch = path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".json");
            boolean isNameMatch = path.contains("openapi") || path.contains("swagger") || path.contains("api-spec") || path.contains("apispec");

            if (isHighPriority) {
                candidates.add(0, file); // prepend
            } else if (isExtensionMatch && isNameMatch) {
                candidates.add(file);
            }
        }

        DiscoveredSpec selectedSpec = null;

        for (RepositorySnapshot.RepositoryFile candidate : candidates) {
            String path = candidate.getPath();
            examined.add(path);

            String content = provider.fetchFileContent(snapshot.getUrl(), snapshot.getHeadCommitSha(), path);
            if (content == null || content.isBlank()) {
                skippedReasons.add("File " + path + " was empty or inaccessible.");
                continue;
            }

            // Verify content contains OpenAPI / Swagger marker
            if (isOpenApiContent(content)) {
                matched.add(path);
                if (selectedSpec == null) {
                    selectedSpec = new DiscoveredSpec(path, content);
                    log.info("Successfully discovered valid OpenAPI document at [{}] ({})", path, selectedSpec.getSpecVersion());
                }
            } else {
                skippedReasons.add("File " + path + " matched naming rules but did not contain an OpenAPI/Swagger header.");
            }
        }

        return new DiscoveryReport(Optional.ofNullable(selectedSpec), examined, matched, skippedReasons);
    }

    private boolean isOpenApiContent(String content) {
        if (content == null || content.length() < 10) return false;
        String preview = content.substring(0, Math.min(content.length(), 4000)).toLowerCase();
        return preview.contains("openapi:") || preview.contains("\"openapi\"")
            || preview.contains("swagger:") || preview.contains("\"swagger\"");
    }
}
