package com.apianalyzer.core.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisJobCompletedEvent {
    private UUID analysisJobId;
    private UUID projectId;
    private String repositoryUrl;
    private String headCommitSha;
}
