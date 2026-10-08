package com.apianalyzer.migration.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "migration_proposals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationProposal {

    public enum ValidationStatus {
        VALID, INVALID, PENDING_VALIDATION
    }

    public enum ApplyStatus {
        GENERATED, APPROVED, REJECTED, APPLIED, STALE, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "analysis_job_id", nullable = false)
    private UUID analysisJobId;

    @Column(name = "project_issue_id", nullable = false)
    private UUID projectIssueId;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "original_content_hash")
    private String originalContentHash;

    @Column(name = "proposed_content", columnDefinition = "TEXT")
    private String proposedContent;

    @Column(name = "unified_diff", columnDefinition = "TEXT")
    private String unifiedDiff;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private ValidationStatus validationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_status", nullable = false)
    private ApplyStatus applyStatus;

    @Column(name = "auto_fix_supported", nullable = false)
    private boolean autoFixSupported;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
