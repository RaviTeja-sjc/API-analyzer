package com.apianalyzer.ingestion.application.service;
import com.apianalyzer.core.domain.entity.*;
import com.apianalyzer.core.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class SpecIngestionService {
    private final OpenApiParserService parserService;
    private final ApiSpecRepository specRepository;
    private final ApiVersionRepository versionRepository;
    private final EndpointRepository endpointRepository;
    private final SourceFileRepository sourceFileRepository;
    private final AuditEventRepository auditRepository;
    
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WRITE')")
    public UUID ingestSpec(UUID projectId, String fileName, String content, UUID userId) {
        // 1. Safe parsing & metadata extraction
        var parsed = parserService.parse(content);
        
        // 2. Resolve/Create Spec
        ApiSpec spec = specRepository.findByProjectIdAndName(projectId, parsed.title)
            .orElseGet(() -> specRepository.save(ApiSpec.builder().projectId(projectId).name(parsed.title).format("OPENAPI").build()));
            
        // 3. Resolve/Create Version
        if(versionRepository.findByApiSpecIdAndVersionTag(spec.getId(), parsed.version).isPresent()) {
            throw new IllegalArgumentException("Version " + parsed.version + " already exists for spec " + parsed.title);
        }
        ApiVersion version = versionRepository.save(ApiVersion.builder().apiSpecId(spec.getId()).versionTag(parsed.version).status("PUBLISHED").build());
        
        // 4. Save Endpoints
        parsed.endpoints.forEach(ep -> endpointRepository.save(Endpoint.builder()
            .apiVersionId(version.getId()).method(ep.method).path(ep.path).operationId(ep.operationId).build()));
            
        // 5. Store Source File Safely with Hash
        sourceFileRepository.save(SourceFile.builder().apiVersionId(version.getId()).filePath(fileName)
            .contentHash(hash(content)).content(content).build());
            
        // 6. Audit Logging
        auditRepository.save(AuditEvent.builder().userId(userId).entityType("ApiVersion").entityId(version.getId()).action("INGESTED").build());
        return version.getId();
    }
    
    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(2 * encoded.length);
            for (byte b : encoded) { hex.append(String.format("%02x", b)); }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }
}
