package com.apianalyzer.core.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "api_versions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "api_spec_id") private UUID apiSpecId;
    private String versionTag;
    private String status;
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
