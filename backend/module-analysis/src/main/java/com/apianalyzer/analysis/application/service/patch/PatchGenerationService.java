package com.apianalyzer.analysis.application.service.patch;
import com.apianalyzer.analysis.domain.model.migration.MigrationSuggestion;
import com.apianalyzer.analysis.domain.model.patch.PatchProposal;
import com.apianalyzer.analysis.domain.model.patch.PatchProposal.Status;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Service
public class PatchGenerationService {
    
    public List<PatchProposal> generatePatches(List<MigrationSuggestion> suggestions, String sourceCode) {
        List<PatchProposal> proposals = new ArrayList<>();
        
        for (MigrationSuggestion suggestion : suggestions) {
            // Only generate patches for high confidence rules that do not require manual review
            if (suggestion.getConfidence() >= 0.90 && !suggestion.isRequiresManualReview()) {
                
                String originalHash = hash(sourceCode);
                String patchedCode = applyPatchLogic(suggestion, sourceCode);
                
                if (!sourceCode.equals(patchedCode)) {
                    String diff = generatePseudoDiff(sourceCode, patchedCode);
                    
                    PatchProposal proposal = PatchProposal.builder()
                        .id(UUID.randomUUID())
                        .fileName(suggestion.getAffectedFile())
                        .originalContentHash(originalHash)
                        .diffContent(diff)
                        .status(Status.PENDING)
                        .rollbackSupported(true)
                        .backupContent(sourceCode)
                        .build();
                        
                    proposals.add(proposal);
                }
            }
        }
        return proposals;
    }
    
    private String applyPatchLogic(MigrationSuggestion suggestion, String sourceCode) {
        // Simplified heuristic: e.g. Schema property removed -> remove the field from DTO.
        // In a production system, this leverages JavaParser LexicalPreservingPrinter 
        // to safely remove the Node from the AST and print it back.
        if (suggestion.getNewUsage().contains("Remove mapping for")) {
            String propName = suggestion.getNewUsage().replace("Remove mapping for ", "").replace(" in DTO", "").trim();
            // Basic string replacement for demonstration (regex removing the private field)
            return sourceCode.replaceAll("(?m)^\\s*private\\s+\\w+\\s+" + propName + "\\s*;\\s*$", "");
        }
        return sourceCode;
    }
    
    private String generatePseudoDiff(String oldCode, String newCode) {
        // Pseudo diff generator for frontend visualization
        return "<<<<<< ORIGINAL\n" + oldCode.substring(0, Math.min(oldCode.length(), 200)) + "...\n" +
               "====== PATCHED\n" + newCode.substring(0, Math.min(newCode.length(), 200)) + "...\n" +
               ">>>>>> END";
    }
    
    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(2 * encoded.length);
            for (byte b : encoded) { hex.append(String.format("%02x", b)); }
            return hex.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
