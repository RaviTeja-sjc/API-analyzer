package com.apianalyzer.migration.presentation.controller;

import com.apianalyzer.migration.domain.entity.MigrationProposal;
import com.apianalyzer.migration.domain.repository.MigrationProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationProposalRepository repository;

    @GetMapping("/analysis/{analysisJobId}/proposals")
    public ResponseEntity<List<MigrationProposal>> getProposalsForAnalysis(@PathVariable UUID analysisJobId) {
        return ResponseEntity.ok(repository.findByAnalysisJobId(analysisJobId));
    }
    
    @GetMapping("/proposals/{proposalId}")
    public ResponseEntity<MigrationProposal> getProposal(@PathVariable UUID proposalId) {
        return repository.findById(proposalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/proposals/{proposalId}/approve")
    public ResponseEntity<MigrationProposal> approveProposal(@PathVariable UUID proposalId) {
        return repository.findById(proposalId).map(proposal -> {
            proposal.setApplyStatus(MigrationProposal.ApplyStatus.APPROVED);
            return ResponseEntity.ok(repository.save(proposal));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/proposals/{proposalId}/reject")
    public ResponseEntity<MigrationProposal> rejectProposal(@PathVariable UUID proposalId) {
        return repository.findById(proposalId).map(proposal -> {
            proposal.setApplyStatus(MigrationProposal.ApplyStatus.REJECTED);
            return ResponseEntity.ok(repository.save(proposal));
        }).orElse(ResponseEntity.notFound().build());
    }
}
