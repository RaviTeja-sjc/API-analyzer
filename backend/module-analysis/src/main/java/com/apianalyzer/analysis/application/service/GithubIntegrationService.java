package com.apianalyzer.analysis.application.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class GithubIntegrationService {
    private final RestTemplate restTemplate = new RestTemplate();

    private HttpEntity<Void> createHttpEntity(String url) {
        HttpHeaders headers = new HttpHeaders();
        if (url != null && url.startsWith("https://api.github.com")) {
            headers.set("Accept", "application/vnd.github.v3+json");
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + token.trim());
            }
        }
        return new HttpEntity<>(headers);
    }

    public List<Map<String, Object>> getLatestCommits(String repoUrl) {
        try {
            Pattern pattern = Pattern.compile("github\\.com/([^/]+)/([^/]+)");
            Matcher matcher = pattern.matcher(repoUrl);
            if (matcher.find()) {
                String owner = matcher.group(1);
                String repo = matcher.group(2).replace(".git", "");
                String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/commits?per_page=5";
                
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, createHttpEntity(apiUrl), new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                if (response.getBody() != null) {
                    return response.getBody();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest commits from GitHub API", e);
        }
        return new ArrayList<>();
    }

    public Map<String, Object> getCommitDetails(String repoUrl, String sha) {
        try {
            Pattern pattern = Pattern.compile("github\\.com/([^/]+)/([^/]+)");
            Matcher matcher = pattern.matcher(repoUrl);
            if (matcher.find()) {
                String owner = matcher.group(1);
                String repo = matcher.group(2).replace(".git", "");
                String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + sha;
                
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, createHttpEntity(apiUrl), new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                if (response.getBody() != null) {
                    return response.getBody();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch commit details from GitHub API", e);
        }
        return null;
    }

    public List<Map<String, Object>> getRepositoryTree(String repoUrl, String branch) {
        try {
            Pattern pattern = Pattern.compile("github\\.com/([^/]+)/([^/]+)");
            Matcher matcher = pattern.matcher(repoUrl);
            if (matcher.find()) {
                String owner = matcher.group(1);
                String repo = matcher.group(2).replace(".git", "");
                String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/" + branch + "?recursive=1";
                
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, createHttpEntity(apiUrl), new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                if (response.getBody() != null && response.getBody().containsKey("tree")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tree = (List<Map<String, Object>>) response.getBody().get("tree");
                    return tree;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch repository tree from GitHub API", e);
        }
        return new ArrayList<>();
    }
    
    public String getFileContent(String owner, String repo, String branch, String path) {
        try {
            String rawUrl = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path;
            ResponseEntity<String> response = restTemplate.getForEntity(rawUrl, String.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
