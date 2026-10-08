package com.apianalyzer.integration.presentation.controller;

import com.apianalyzer.core.domain.entity.VcsConnection;
import com.apianalyzer.core.domain.repository.VcsConnectionRepository;
import com.apianalyzer.integration.application.dto.MigrationRequestDto;
import com.apianalyzer.integration.infrastructure.github.GithubMigrationClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/integrations/github")
public class GithubMigrationController {

    private final GithubMigrationClient githubMigrationClient;
    private final VcsConnectionRepository vcsConnectionRepository;

    public GithubMigrationController(GithubMigrationClient githubMigrationClient, VcsConnectionRepository vcsConnectionRepository) {
        this.githubMigrationClient = githubMigrationClient;
        this.vcsConnectionRepository = vcsConnectionRepository;
    }

    @PostMapping("/migrate")
    public ResponseEntity<String> executeMigration(@RequestBody MigrationRequestDto request) {
        Optional<VcsConnection> connectionOpt = vcsConnectionRepository.findByProjectId(request.getProjectId());
        
        if (connectionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("No VCS connection found for the specified project.");
        }
        
        VcsConnection connection = connectionOpt.get();
        // getEncryptedToken() actually returns decrypted token in Java due to JPA @Convert
        String pat = connection.getEncryptedToken();

        if (pat == null || pat.isBlank()) {
            return ResponseEntity.badRequest().body("VCS connection token is missing.");
        }

        try {
            String prUrl = githubMigrationClient.createMigrationPullRequest(
                    request.getOwner(),
                    request.getRepo(),
                    request.getBaseBranch(),
                    request.getFilePath(),
                    request.getPatchedContent(),
                    request.getCommitMessage(),
                    request.getPrTitle(),
                    request.getPrBody(),
                    pat
            );
            return ResponseEntity.ok(prUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Migration failed: " + e.getMessage());
        }
    }
}
