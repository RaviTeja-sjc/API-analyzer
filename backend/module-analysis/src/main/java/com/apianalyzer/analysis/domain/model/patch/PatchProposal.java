package com.apianalyzer.analysis.domain.model.patch;
import lombok.*;
import java.util.UUID;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatchProposal {
    public enum Status { PENDING, APPROVED, APPLIED, ROLLED_BACK }
    
    private UUID id;
    private String fileName;
    private String originalContentHash;
    private String diffContent; // Unified diff or Before/After format
    private Status status;
    private boolean rollbackSupported;
    private String backupContent;
}
