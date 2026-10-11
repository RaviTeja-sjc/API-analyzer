package com.apianalyzer.analysis.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RepositoryAnalysisOrchestrator {

    private final AsyncAnalysisOrchestrator asyncAnalysisOrchestrator;

    @Async
    public CompletableFuture<Void> processRepositoryAnalysisAsync(UUID jobId, UUID projectId) {
        return asyncAnalysisOrchestrator.processJobAsync(jobId);
    }
}
