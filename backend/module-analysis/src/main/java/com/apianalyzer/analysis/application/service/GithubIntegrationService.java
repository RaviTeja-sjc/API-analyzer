package com.apianalyzer.analysis.application.service;

import com.apianalyzer.analysis.domain.exception.GithubApiException;
import com.apianalyzer.analysis.domain.exception.GithubRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GithubIntegrationService {

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/]+?)(?:\\.git)?(?:/.*)?$");
    private final RestTemplate restTemplate;

    @Value("${github.token:${GITHUB_TOKEN:}}")
    private String configuredToken;

    public GithubIntegrationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public static class RepoCoordinates {
        private final String owner;
        private final String repo;

        public RepoCoordinates(String owner, String repo) {
            this.owner = owner;
            this.repo = repo;
        }

        public String getOwner() { return owner; }
        public String getRepo() { return repo; }
    }

    public RepoCoordinates parseCoordinates(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL cannot be null or empty");
        }
        Matcher matcher = GITHUB_URL_PATTERN.matcher(repoUrl.trim());
        if (matcher.find()) {
            String owner = matcher.group(1).trim();
            String repo = matcher.group(2).trim();
            return new RepoCoordinates(owner, repo);
        }
        throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
    }

    private String resolveToken() {
        if (configuredToken != null && !configuredToken.trim().isEmpty()) {
            return configuredToken.trim();
        }
        String envToken = System.getenv("GITHUB_TOKEN");
        if (envToken != null && !envToken.trim().isEmpty()) {
            return envToken.trim();
        }
        return null;
    }

    private <T> HttpEntity<T> createHttpEntity(String url, String acceptHeader, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "API-Analyzer-Service/1.0 (https://github.com/RaviTeja-sjc/API-analyzer)");
        if (acceptHeader != null) {
            headers.set("Accept", acceptHeader);
        } else {
            headers.set("Accept", "application/vnd.github.v3+json");
        }

        String token = resolveToken();
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        return new HttpEntity<>(body, headers);
    }

    private void checkRateLimitHeaders(HttpHeaders headers, String endpoint) {
        if (headers == null) return;
        List<String> remainingList = headers.get("X-RateLimit-Remaining");
        List<String> resetList = headers.get("X-RateLimit-Reset");
        if (remainingList != null && !remainingList.isEmpty()) {
            try {
                int remaining = Integer.parseInt(remainingList.get(0));
                long resetEpoch = (resetList != null && !resetList.isEmpty()) ? Long.parseLong(resetList.get(0)) : 0L;
                log.debug("GitHub API [{}] rate limit remaining: {} (reset at epoch {})", endpoint, remaining, resetEpoch);
                if (remaining <= 0 && resetEpoch > 0) {
                    Instant resetTime = Instant.ofEpochSecond(resetEpoch);
                    log.warn("GitHub API rate limit exhausted for endpoint [{}]. Reset at {}", endpoint, resetTime);
                    throw new GithubRateLimitException("GitHub API rate limit exhausted. Resets at " + resetTime, resetEpoch, remaining);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private <T> ResponseEntity<T> executeWithRetry(String url, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType, String safeEndpointName) {
        int maxRetries = 3;
        long backoffMs = 500;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Calling GitHub API [{}], attempt {}/{}", safeEndpointName, attempt, maxRetries);
                ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);
                checkRateLimitHeaders(response.getHeaders(), safeEndpointName);
                return response;
            } catch (HttpClientErrorException.Forbidden e) {
                HttpHeaders headers = e.getResponseHeaders();
                String retryAfter = headers != null ? headers.getFirst("Retry-After") : null;
                String remaining = headers != null ? headers.getFirst("X-RateLimit-Remaining") : null;
                String reset = headers != null ? headers.getFirst("X-RateLimit-Reset") : null;

                if ("0".equals(remaining) || retryAfter != null || (e.getResponseBodyAsString().toLowerCase().contains("rate limit"))) {
                    long resetEpoch = reset != null ? Long.parseLong(reset) : (System.currentTimeMillis() / 1000 + 60);
                    Instant resetTime = Instant.ofEpochSecond(resetEpoch);
                    log.warn("GitHub rate limit reached on [{}] (HTTP 403). Reset at {}", safeEndpointName, resetTime);
                    throw new GithubRateLimitException("GitHub API rate limit exceeded. Reset at: " + resetTime, resetEpoch, 0);
                }
                throw new GithubApiException("Access forbidden (403) to GitHub repository [" + safeEndpointName + "]: " + e.getStatusText(), 403, safeEndpointName, e);
            } catch (HttpClientErrorException.TooManyRequests e) {
                HttpHeaders headers = e.getResponseHeaders();
                String reset = headers != null ? headers.getFirst("X-RateLimit-Reset") : null;
                long resetEpoch = reset != null ? Long.parseLong(reset) : (System.currentTimeMillis() / 1000 + 60);
                throw new GithubRateLimitException("GitHub API secondary rate limit (429) hit. Resets around " + Instant.ofEpochSecond(resetEpoch), resetEpoch, 0);
            } catch (HttpClientErrorException.NotFound e) {
                throw new GithubApiException("GitHub resource not found (404) at [" + safeEndpointName + "]. Ensure repo exists and is accessible.", 404, safeEndpointName, e);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new GithubApiException("GitHub API authentication failed (401). Invalid or expired GITHUB_TOKEN.", 401, safeEndpointName, e);
            } catch (HttpClientErrorException e) {
                throw new GithubApiException("GitHub API client error (" + e.getStatusCode().value() + ") on [" + safeEndpointName + "]: " + e.getStatusText(), e.getStatusCode().value(), safeEndpointName, e);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                lastException = e;
                log.warn("Transient error on GitHub API call [{}] (attempt {}/{}): {}", safeEndpointName, attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        long jitter = (long) (Math.random() * 200);
                        Thread.sleep(backoffMs + jitter);
                        backoffMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new GithubApiException("Interrupted during retry backoff", 500, safeEndpointName, ie);
                    }
                }
            }
        }
        throw new GithubApiException("GitHub API failed after " + maxRetries + " attempts for [" + safeEndpointName + "]", 503, safeEndpointName, lastException);
    }

    public Map<String, Object> getRepositoryMetadata(String repoUrl) {
        RepoCoordinates coords = parseCoordinates(repoUrl);
        String apiUrl = "https://api.github.com/repos/" + coords.getOwner() + "/" + coords.getRepo();
        String safeName = coords.getOwner() + "/" + coords.getRepo();
        ResponseEntity<Map<String, Object>> response = executeWithRetry(
                apiUrl, HttpMethod.GET, createHttpEntity(apiUrl, "application/vnd.github.v3+json", null),
                new ParameterizedTypeReference<Map<String, Object>>() {}, safeName
        );
        return response.getBody() != null ? response.getBody() : Collections.emptyMap();
    }

    public String resolveDefaultBranch(String repoUrl) {
        try {
            Map<String, Object> meta = getRepositoryMetadata(repoUrl);
            if (meta.containsKey("default_branch") && meta.get("default_branch") != null) {
                return (String) meta.get("default_branch");
            }
        } catch (Exception e) {
            log.warn("Could not query default branch from GitHub metadata for {}: {}. Will fallback to HEAD commits.", repoUrl, e.getMessage());
        }
        return "main";
    }

    public List<Map<String, Object>> getLatestCommits(String repoUrl) {
        RepoCoordinates coords = parseCoordinates(repoUrl);
        String apiUrl = "https://api.github.com/repos/" + coords.getOwner() + "/" + coords.getRepo() + "/commits?per_page=15";
        String safeName = coords.getOwner() + "/" + coords.getRepo() + "/commits";

        ResponseEntity<List<Map<String, Object>>> response = executeWithRetry(
                apiUrl, HttpMethod.GET, createHttpEntity(apiUrl, "application/vnd.github.v3+json", null),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}, safeName
        );
        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    public Map<String, Object> getCommitDetails(String repoUrl, String sha) {
        RepoCoordinates coords = parseCoordinates(repoUrl);
        String apiUrl = "https://api.github.com/repos/" + coords.getOwner() + "/" + coords.getRepo() + "/commits/" + sha;
        String safeName = coords.getOwner() + "/" + coords.getRepo() + "/commit/" + sha;

        ResponseEntity<Map<String, Object>> response = executeWithRetry(
                apiUrl, HttpMethod.GET, createHttpEntity(apiUrl, "application/vnd.github.v3+json", null),
                new ParameterizedTypeReference<Map<String, Object>>() {}, safeName
        );
        return response.getBody();
    }

    public String resolveBranchOrCommitToSha(String repoUrl, String branchOrRef) {
        if (branchOrRef == null || branchOrRef.isBlank() || "HEAD".equalsIgnoreCase(branchOrRef)) {
            List<Map<String, Object>> commits = getLatestCommits(repoUrl);
            if (!commits.isEmpty()) {
                return (String) commits.get(0).get("sha");
            }
            return resolveDefaultBranch(repoUrl);
        }
        if ("HEAD~1".equalsIgnoreCase(branchOrRef)) {
            List<Map<String, Object>> commits = getLatestCommits(repoUrl);
            if (commits.size() > 1) {
                return (String) commits.get(1).get("sha");
            } else if (commits.size() == 1) {
                return (String) commits.get(0).get("sha");
            }
            return resolveDefaultBranch(repoUrl);
        }
        // If it's already a 40-char SHA
        if (branchOrRef.matches("^[0-9a-fA-F]{40}$")) {
            return branchOrRef;
        }
        return branchOrRef;
    }

    public List<Map<String, Object>> getRepositoryTree(String repoUrl, String branchOrRef) {
        RepoCoordinates coords = parseCoordinates(repoUrl);
        String ref = resolveBranchOrCommitToSha(repoUrl, branchOrRef);

        String apiUrl = "https://api.github.com/repos/" + coords.getOwner() + "/" + coords.getRepo() + "/git/trees/" + ref + "?recursive=1";
        String safeName = coords.getOwner() + "/" + coords.getRepo() + "/tree/" + ref;

        ResponseEntity<Map<String, Object>> response = executeWithRetry(
                apiUrl, HttpMethod.GET, createHttpEntity(apiUrl, "application/vnd.github.v3+json", null),
                new ParameterizedTypeReference<Map<String, Object>>() {}, safeName
        );

        if (response.getBody() != null && response.getBody().containsKey("tree")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tree = (List<Map<String, Object>>) response.getBody().get("tree");
            return tree != null ? tree : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public String getFileContent(String owner, String repo, String ref, String path) {
        if (path == null || path.isBlank()) return null;
        String safeRef = ref != null && !ref.isBlank() ? ref : "HEAD";

        // Strategy 1: GitHub API with Accept: application/vnd.github.v3.raw (authenticated, handles all refs)
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path + "?ref=" + safeRef;
        String safeName = owner + "/" + repo + "/" + path;

        try {
            ResponseEntity<String> response = executeWithRetry(
                    apiUrl, HttpMethod.GET, createHttpEntity(apiUrl, "application/vnd.github.v3.raw", null),
                    new ParameterizedTypeReference<String>() {}, safeName
            );
            if (response.getBody() != null) {
                return response.getBody();
            }
        } catch (GithubRateLimitException e) {
            throw e; // rethrow rate limits
        } catch (Exception e) {
            log.debug("Direct raw GitHub contents call failed for {}: {}. Attempting fallback...", safeName, e.getMessage());
        }

        // Strategy 2: GitHub API standard JSON with base64 decoding
        try {
            ResponseEntity<Map<String, Object>> jsonResponse = executeWithRetry(
                    apiUrl, HttpMethod.GET, createHttpEntity(apiUrl, "application/vnd.github.v3+json", null),
                    new ParameterizedTypeReference<Map<String, Object>>() {}, safeName
            );
            if (jsonResponse.getBody() != null && jsonResponse.getBody().containsKey("content")) {
                String encoded = (String) jsonResponse.getBody().get("content");
                if (encoded != null) {
                    encoded = encoded.replaceAll("\\s+", "");
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    return new String(decoded, StandardCharsets.UTF_8);
                }
            }
        } catch (GithubRateLimitException e) {
            throw e;
        } catch (Exception ignored) {}

        // Strategy 3: raw.githubusercontent.com fallback with auth header
        try {
            String rawUrl = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + safeRef + "/" + path;
            ResponseEntity<String> response = executeWithRetry(
                    rawUrl, HttpMethod.GET, createHttpEntity(rawUrl, "text/plain", null),
                    new ParameterizedTypeReference<String>() {}, "raw/" + safeName
            );
            return response.getBody();
        } catch (Exception e) {
            log.debug("Failed to retrieve content for {}: {}", safeName, e.getMessage());
            return null;
        }
    }
}
