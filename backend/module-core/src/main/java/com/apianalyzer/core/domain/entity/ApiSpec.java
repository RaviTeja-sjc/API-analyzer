package com.apianalyzer.core.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "api_specs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiSpec {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "project_id") private UUID projectId;
    private String name;
    private String format;
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
