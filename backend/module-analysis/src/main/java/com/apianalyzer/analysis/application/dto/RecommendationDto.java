package com.apianalyzer.analysis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDto {
    private UUID projectIssueId;
    private String ruleId;
    private String category;
    private String title;
    private String explanation;
    private String remediation;
    private String priority;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
    private String evidence;
}
