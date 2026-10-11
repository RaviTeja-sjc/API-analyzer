package com.apianalyzer.analysis.infrastructure.repository;

import com.apianalyzer.analysis.application.service.GithubIntegrationService;
import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GithubRepositoryProvider implements RepositoryProvider {

    private final GithubIntegrationService githubIntegrationService;

    @Override
    public RepositorySnapshot fetchSnapshot(String repoUrl, String branchOrCommit) {
        List<Map<String, Object>> tree = githubIntegrationService.getRepositoryTree(repoUrl, branchOrCommit);
        
        List<RepositorySnapshot.RepositoryFile> files = tree.stream()
            .map(node -> RepositorySnapshot.RepositoryFile.builder()
                .path((String) node.get("path"))
                .type((String) node.get("type"))
                .size(node.get("size") != null ? ((Number) node.get("size")).longValue() : 0L)
                .build())
            .collect(Collectors.toList());

        String resolvedSha = githubIntegrationService.resolveBranchOrCommitToSha(repoUrl, branchOrCommit);

        return RepositorySnapshot.builder()
            .url(repoUrl)
            .headCommitSha(resolvedSha)
            .files(files)
            .build();
    }

    @Override
    public String fetchFileContent(String repoUrl, String commitSha, String path) {
        try {
            var coords = githubIntegrationService.parseCoordinates(repoUrl);
            return githubIntegrationService.getFileContent(coords.getOwner(), coords.getRepo(), commitSha, path);
        } catch (Exception e) {
            return null;
        }
    }
}
