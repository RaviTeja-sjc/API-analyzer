package com.apianalyzer.migration.application.service;

import com.apianalyzer.core.domain.entity.ProjectIssue;
import com.apianalyzer.core.domain.event.AnalysisJobCompletedEvent;
import com.apianalyzer.core.domain.repository.ProjectIssueRepository;
import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisJobCompletedEventListener {

    private final ProjectIssueRepository projectIssueRepository;
    private final PatchGenerationService patchGenerationService;
    private final RepositoryProvider repositoryProvider;

    @Async
    @EventListener
    public void handleAnalysisJobCompleted(AnalysisJobCompletedEvent event) {
        log.info("Generating migration proposals for analysis job {}", event.getAnalysisJobId());
        
        try {
            List<ProjectIssue> issues = projectIssueRepository.findByAnalysisJobId(event.getAnalysisJobId());
            if (issues.isEmpty()) {
                log.info("No issues found for job {}. Skipping migration proposals generation.", event.getAnalysisJobId());
                return;
            }

            RepositorySnapshot snapshot = repositoryProvider.fetchSnapshot(event.getRepositoryUrl(), event.getHeadCommitSha());
            patchGenerationService.generateProposals(issues, snapshot);
            log.info("Finished generating migration proposals for job {}", event.getAnalysisJobId());
        } catch (Exception e) {
            log.error("Failed to generate migration proposals for job {}: {}", event.getAnalysisJobId(), e.getMessage(), e);
        }
    }
}
