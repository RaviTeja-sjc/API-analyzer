package com.apianalyzer.integration.infrastructure.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"unchecked", "null", "rawtypes"})
public class GithubMigrationClient {

    private static final Logger log = LoggerFactory.getLogger(GithubMigrationClient.class);
    private static final String GITHUB_API_URL = "https://api.github.com/repos/";
    private final RestTemplate restTemplate;

    public GithubMigrationClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Executes the "Push-to-Fix" workflow: Creates a branch, commits the migrated file, and opens a Pull Request.
     */
    public String createMigrationPullRequest(String owner, String repo, String baseBranch, 
                                             String filePath, String patchedContent, 
                                             String commitMessage, String prTitle, 
                                             String prBody, String decryptedPat) {
        
        String newBranchName = "api-analyzer-migration-" + System.currentTimeMillis();
        HttpHeaders headers = createAuthHeaders(decryptedPat);

        try {
            log.info("Starting Push-to-Fix workflow for {}/{}", owner, repo);
            
            // 1. Get SHA of Base Branch to branch off from
            String baseSha = getBranchSha(owner, repo, baseBranch, headers);
            
            // 2. Create New Branch
            createBranch(owner, repo, newBranchName, baseSha, headers);
            
            // 3. Get SHA of the file we are modifying (required by GitHub API to update a file)
            String fileSha = getFileSha(owner, repo, filePath, newBranchName, headers);
            
            // 4. Commit the patched content to the new branch
            commitFile(owner, repo, filePath, patchedContent, commitMessage, fileSha, newBranchName, headers);
            
            // 5. Open the Pull Request
            return openPullRequest(owner, repo, newBranchName, baseBranch, prTitle, prBody, headers);

        } catch (Exception e) {
            log.error("Failed to execute GitHub Push-to-Fix workflow: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create migration PR: " + e.getMessage());
        }
    }

    private HttpHeaders createAuthHeaders(String pat) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + pat);
        headers.set("Accept", "application/vnd.github.v3+json");
        return headers;
    }

    private String getBranchSha(String owner, String repo, String branch, HttpHeaders headers) {
        String url = GITHUB_API_URL + owner + "/" + repo + "/git/ref/heads/" + branch;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> objectNode = (Map<String, Object>) response.getBody().get("object");
        return (String) objectNode.get("sha");
    }

    private void createBranch(String owner, String repo, String newBranch, String baseSha, HttpHeaders headers) {
        String url = GITHUB_API_URL + owner + "/" + repo + "/git/refs";
        Map<String, String> body = new HashMap<>();
        body.put("ref", "refs/heads/" + newBranch);
        body.put("sha", baseSha);
        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        log.info("Created new branch: {}", newBranch);
    }

    private String getFileSha(String owner, String repo, String filePath, String branch, HttpHeaders headers) {
        String url = GITHUB_API_URL + owner + "/" + repo + "/contents/" + filePath + "?ref=" + branch;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return (String) response.getBody().get("sha");
        } catch (Exception e) {
            // File might not exist, or API rate limit. Assume it exists since we analyzed it.
            log.warn("Could not fetch SHA for file {}, assuming new file or error: {}", filePath, e.getMessage());
            return null;
        }
    }

    private void commitFile(String owner, String repo, String filePath, String content, 
                            String message, String sha, String branch, HttpHeaders headers) {
        String url = GITHUB_API_URL + owner + "/" + repo + "/contents/" + filePath;
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes()));
        body.put("branch", branch);
        if (sha != null) {
            body.put("sha", sha);
        }
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
        log.info("Committed patch to file: {}", filePath);
    }

    private String openPullRequest(String owner, String repo, String headBranch, String baseBranch, 
                                   String title, String bodyText, HttpHeaders headers) {
        String url = GITHUB_API_URL + owner + "/" + repo + "/pulls";
        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("head", headBranch);
        body.put("base", baseBranch);
        body.put("body", bodyText);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        String prUrl = (String) response.getBody().get("html_url");
        log.info("Successfully created Pull Request: {}", prUrl);
        return prUrl;
    }
}
