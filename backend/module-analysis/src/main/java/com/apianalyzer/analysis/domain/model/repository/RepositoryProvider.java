package com.apianalyzer.analysis.domain.model.repository;

public interface RepositoryProvider {
    RepositorySnapshot fetchSnapshot(String repoUrl, String branchOrCommit);
    String fetchFileContent(String repoUrl, String commitSha, String path);
}
