package com.apianalyzer.core.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name = "vcs_connections")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VcsConnection {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "project_id") private UUID projectId;
    private String provider; // GITHUB, GITLAB, BITBUCKET
    private String repositoryUrl;
    
    // In a real production app, this must be encrypted using an AES converter
    @Column(name = "encrypted_token")
    @Convert(converter = com.apianalyzer.core.security.encryption.AttributeEncryptor.class)
    private String encryptedToken;
    
    private String defaultBranch;
    private String specFilePath;
    private String sourceDirectory;
    private boolean isValidated;
}
