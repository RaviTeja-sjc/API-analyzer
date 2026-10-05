package com.apianalyzer.ingestion.presentation.controller;
import com.apianalyzer.core.domain.entity.User;
import com.apianalyzer.ingestion.application.service.SpecIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/projects/{projectId}/specs")
@RequiredArgsConstructor
public class IngestionController {
    private final SpecIngestionService ingestionService;
    
    @PostMapping
    public ResponseEntity<?> uploadSpec(@PathVariable UUID projectId, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        String content = new String(file.getBytes());
        UUID versionId = ingestionService.ingestSpec(projectId, file.getOriginalFilename(), content, user.getId());
        return ResponseEntity.ok(Map.of("message", "Ingestion successful", "versionId", versionId));
    }
}
