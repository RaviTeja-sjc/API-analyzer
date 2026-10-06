package com.apianalyzer.ingestion.infrastructure.vcs;
import com.apianalyzer.core.domain.entity.VcsConnection;
import com.apianalyzer.ingestion.application.service.vcs.VcsProvider;
import org.springframework.stereotype.Component;
import java.util.List;
// Placeholder for actual HttpClient calls to api.github.com
@Component
public class GithubProvider implements VcsProvider {
    @Override
    public boolean supports(String providerType) {
        return "GITHUB".equalsIgnoreCase(providerType);
    }
    
    @Override
    public boolean validateAccess(VcsConnection connection) {
        // e.g., GET https://api.github.com/repos/{owner}/{repo} with Bearer token
        return connection.getEncryptedToken() != null && !connection.getEncryptedToken().isEmpty();
    }
    
    @Override
    public List<String> listBranches(VcsConnection connection) {
        return List.of("main", "develop", "feature/api-v2");
    }
    
    @Override
    public String fetchFileContent(VcsConnection connection, String branch, String filePath) {
        // e.g., GET https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{filePath}
        return "mock_file_content_from_github";
    }
    
    @Override
    public List<String> listSourceDirectories(VcsConnection connection, String branch) {
        return List.of("src/main/java", "src/test/java");
    }
}
