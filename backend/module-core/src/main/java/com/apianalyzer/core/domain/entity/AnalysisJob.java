package com.apianalyzer.core.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "analysis_jobs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisJob {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "project_id") private UUID projectId;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private int progress;
    @Column(length = 4000) private String logs;
    private String idempotencyKey;
    @Column(name = "report_id") private UUID reportId;
    @Column(name = "result_payload", columnDefinition = "TEXT") private String resultPayload;
    
    // Coverage metrics
    @Column(name = "total_files_discovered") private Integer totalFilesDiscovered;
    @Column(name = "files_analyzed") private Integer filesAnalyzed;
    @Column(name = "files_skipped") private Integer filesSkipped;
    @Column(name = "unsupported_files") private Integer unsupportedFiles;
    @Column(name = "languages", columnDefinition = "TEXT") private String languages;

    // Scores
    @Column(name = "security_score") private Integer securityScore;
    @Column(name = "api_health_score") private Integer apiHealthScore;
    @Column(name = "score_breakdown", columnDefinition = "TEXT") private String scoreBreakdown;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    public void appendLog(String log) {
        this.logs = (this.logs == null ? "" : this.logs + "\n") + log;
    }
}
