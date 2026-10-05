package com.apianalyzer.core.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name = "endpoints")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Endpoint {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "api_version_id") private UUID apiVersionId;
    private String method;
    private String path;
    private String operationId;
}
