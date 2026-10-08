package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApiSpecDiscoveryService {

    public static class DiscoveredSpec {
        private final String filePath;
        private final String content;
        public DiscoveredSpec(String filePath, String content) {
            this.filePath = filePath;
            this.content = content;
        }
        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
    }

    public Optional<DiscoveredSpec> discoverOpenApiSpec(RepositoryProvider provider, RepositorySnapshot snapshot) {
        String[] possiblePaths = {
            "openapi.yaml", "openapi.yml", "openapi.json",
            "swagger.yaml", "swagger.yml", "swagger.json",
            "api/openapi.yaml", "api/openapi.yml", "api/openapi.json",
            "docs/openapi.yaml", "docs/openapi.yml", "docs/openapi.json"
        };
        
        for (RepositorySnapshot.RepositoryFile file : snapshot.getFiles()) {
            for (String p : possiblePaths) {
                if (file.getPath().equalsIgnoreCase(p)) {
                    String content = provider.fetchFileContent(snapshot.getUrl(), snapshot.getHeadCommitSha(), file.getPath());
                    if (content != null) {
                        return Optional.of(new DiscoveredSpec(file.getPath(), content));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
