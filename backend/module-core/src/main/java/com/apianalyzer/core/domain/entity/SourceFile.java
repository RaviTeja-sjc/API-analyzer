package com.apianalyzer.core.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name = "source_files")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceFile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "api_version_id") private UUID apiVersionId;
    private String filePath;
    private String contentHash;
    private String content;
}
