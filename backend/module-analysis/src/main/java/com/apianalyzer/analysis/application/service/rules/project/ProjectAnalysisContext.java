package com.apianalyzer.analysis.application.service.rules.project;

import com.apianalyzer.analysis.domain.model.repository.RepositoryProvider;
import com.apianalyzer.analysis.domain.model.repository.RepositorySnapshot;
import com.apianalyzer.core.domain.entity.Project;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ProjectAnalysisContext {
    private final UUID projectId;
    private final UUID analysisJobId;
    private final Project project;
    private final RepositorySnapshot snapshot;
    private final RepositoryProvider repositoryProvider;
}
