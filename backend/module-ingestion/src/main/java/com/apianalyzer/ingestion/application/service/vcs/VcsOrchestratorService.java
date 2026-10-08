package com.apianalyzer.ingestion.application.service.vcs;
import com.apianalyzer.core.domain.entity.VcsConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class VcsOrchestratorService {
    private final List<VcsProvider> providers;
    
    public VcsProvider getProvider(String providerType) {
        return providers.stream()
            .filter(p -> p.supports(providerType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported VCS provider: " + providerType));
    }
    
    public void testConnectionAndAudit(VcsConnection connection) {
        VcsProvider provider = getProvider(connection.getProvider());
        boolean isValid = provider.validateAccess(connection);
        connection.setValidated(isValid);
        
        // Pseudo Audit Log
        log.info("AUDIT: VCS Connection tested for " + connection.getRepositoryUrl() + ". Success: " + isValid);
        
        if (!isValid) {
            throw new SecurityException("VCS Access Validation Failed. Check credentials and repository URL.");
        }
    }
}
