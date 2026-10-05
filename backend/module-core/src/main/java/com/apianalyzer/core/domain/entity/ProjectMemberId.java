package com.apianalyzer.core.domain.entity;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;
@Embeddable
@Data @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ProjectMemberId implements Serializable {
    private UUID projectId;
    private UUID userId;
}
