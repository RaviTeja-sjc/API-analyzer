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

        return RepositorySnapshot.builder()
            .url(repoUrl)
            .headCommitSha(branchOrCommit) // Ideally resolve branch to sha
            .files(files)
            .build();
    }

    @Override
    public String fetchFileContent(String repoUrl, String commitSha, String path) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("github\\.com/([^/]+)/([^/]+)");
        java.util.regex.Matcher matcher = pattern.matcher(repoUrl);
        if (matcher.find()) {
            String owner = matcher.group(1);
            String repo = matcher.group(2).replace(".git", "");
            return githubIntegrationService.getFileContent(owner, repo, commitSha, path);
        }
        return null;
    }
}
