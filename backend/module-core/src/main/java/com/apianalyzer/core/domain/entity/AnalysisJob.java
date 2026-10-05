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
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    public void appendLog(String log) {
        this.logs = (this.logs == null ? "" : this.logs + "\n") + log;
    }
}
