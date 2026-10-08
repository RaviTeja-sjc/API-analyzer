package com.apianalyzer.core.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class ProjectCreatedEvent extends ApplicationEvent {
    private final UUID projectId;
    private final String repositoryUrl;

    public ProjectCreatedEvent(Object source, UUID projectId, String repositoryUrl) {
        super(source);
        this.projectId = projectId;
        this.repositoryUrl = repositoryUrl;
    }
}
