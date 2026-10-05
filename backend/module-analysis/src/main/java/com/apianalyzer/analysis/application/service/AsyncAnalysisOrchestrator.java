package com.apianalyzer.analysis.application.service;
import com.apianalyzer.core.domain.entity.AnalysisJob;
import com.apianalyzer.core.domain.repository.AnalysisJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
@Service
@RequiredArgsConstructor
public class AsyncAnalysisOrchestrator {
    private final AnalysisJobRepository jobRepository;
    // Assuming dependencies for DiffEngine, DetectionService, etc. are injected here in reality
    
    @Transactional
    public AnalysisJob submitJob(UUID projectId, String baseVer, String headVer, String idempotencyKey) {
        // Idempotency check: don't start a new job if one is already RUNNING or PENDING for this specific key
        var existing = jobRepository.findByIdempotencyKeyAndStatusIn(idempotencyKey, List.of("PENDING", "RUNNING", "COMPLETED"));
        if (existing.isPresent()) {
            return existing.get();
        }
        
        AnalysisJob job = AnalysisJob.builder()
            .projectId(projectId)
            .status("PENDING")
            .progress(0)
            .idempotencyKey(idempotencyKey)
            .logs("Job submitted for " + baseVer + " -> " + headVer)
            .build();
        return jobRepository.save(job);
    }
    
    @Async
    public CompletableFuture<Void> processJobAsync(UUID jobId) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow();
        if (job.getStatus().equals("CANCELLED")) return CompletableFuture.completedFuture(null);
        
        try {
            updateStatus(job, "RUNNING", 10, "Starting analysis pipeline...");
            
            // Step 1: Parse Specs (Simulated delay)
            updateStatus(job, "RUNNING", 30, "Parsing OpenAPI specifications...");
            Thread.sleep(1000);
            
            if (isCancelled(jobId)) return CompletableFuture.completedFuture(null);
            
            // Step 2: Diff Engine
            updateStatus(job, "RUNNING", 50, "Executing Semantic Diff Engine...");
            Thread.sleep(1000);
            
            // Step 3: AST Consumer Detection
            updateStatus(job, "RUNNING", 75, "Running AST Consumer Detection & Graph Builder...");
            Thread.sleep(1500);
            
            // Step 4: Report Building
            updateStatus(job, "RUNNING", 95, "Generating Structured Report...");
            
            // Finalize
            updateStatus(job, "COMPLETED", 100, "Analysis completed successfully.");
            
        } catch (InterruptedException e) {
            updateStatus(job, "FAILED", job.getProgress(), "Job interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            updateStatus(job, "FAILED", job.getProgress(), "Fatal error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    public void cancelJob(UUID jobId) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow();
        if (job.getStatus().equals("PENDING") || job.getStatus().equals("RUNNING")) {
            updateStatus(job, "CANCELLED", job.getProgress(), "Job was manually cancelled.");
        }
    }
    
    private void updateStatus(AnalysisJob job, String status, int progress, String log) {
        job.setStatus(status);
        job.setProgress(progress);
        job.appendLog(log);
        jobRepository.save(job);
    }
    
    private boolean isCancelled(UUID jobId) {
        return jobRepository.findById(jobId).map(j -> j.getStatus().equals("CANCELLED")).orElse(true);
    }
}

