package com.apianalyzer.analysis.domain.model.repository;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RepositorySnapshot {
    private String url;
    private String headCommitSha;
    private List<RepositoryFile> files;

    @Data
    @Builder
    public static class RepositoryFile {
        private String path;
        private String type; // blob, tree, etc.
        private long size;
        private String content; // populated when fetched
    }
}
