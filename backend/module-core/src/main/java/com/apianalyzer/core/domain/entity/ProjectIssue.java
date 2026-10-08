package com.apianalyzer.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectIssue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    
    @Column(name = "analysis_job_id")
    private UUID analysisJobId;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "rule_id", nullable = false)
    private String ruleId;
    
    @Column(nullable = false)
    private String severity;
    
    @Column(nullable = false)
    private String confidence;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(name = "problem_description", nullable = false, columnDefinition = "TEXT")
    private String problemDescription;
    
    @Column(columnDefinition = "TEXT")
    private String impact;
    
    @Column(columnDefinition = "TEXT")
    private String recommendation;
    
    @Column(name = "file_path", length = 1000)
    private String filePath;
    
    @Column(name = "start_line")
    private Integer startLine;
    
    @Column(name = "end_line")
    private Integer endLine;
    
    @Column(columnDefinition = "TEXT")
    private String evidence;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
