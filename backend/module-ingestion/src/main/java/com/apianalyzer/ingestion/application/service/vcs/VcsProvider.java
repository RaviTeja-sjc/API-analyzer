package com.apianalyzer.ingestion.application.service.vcs;
import com.apianalyzer.core.domain.entity.VcsConnection;
import java.util.List;
public interface VcsProvider {
    boolean supports(String providerType);
    boolean validateAccess(VcsConnection connection);
    List<String> listBranches(VcsConnection connection);
    String fetchFileContent(VcsConnection connection, String branch, String filePath);
    List<String> listSourceDirectories(VcsConnection connection, String branch);
}
