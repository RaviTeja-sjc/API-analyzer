package com.apianalyzer.analysis.application.service;

import com.apianalyzer.core.domain.event.ProjectCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventListener {

    private final AsyncAnalysisOrchestrator analysisOrchestrator;

    @Async
    @EventListener
    public void handleProjectCreatedEvent(ProjectCreatedEvent event) {
        log.info("Received ProjectCreatedEvent for project: {} with URL: {}. Triggering AST Analysis pipeline...", 
                 event.getProjectId(), event.getRepositoryUrl());
                 
        // Submit and trigger the asynchronous analysis orchestrator
        com.apianalyzer.core.domain.entity.AnalysisJob job = analysisOrchestrator.submitJob(
                event.getProjectId(), "v1.0", "HEAD", UUID.randomUUID().toString()
        );
        analysisOrchestrator.processJobAsync(job.getId());
    }
}
