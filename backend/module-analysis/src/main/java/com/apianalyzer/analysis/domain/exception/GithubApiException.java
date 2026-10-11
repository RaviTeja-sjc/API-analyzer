package com.apianalyzer.analysis.domain.exception;

import lombok.Getter;

@Getter
public class GithubApiException extends RuntimeException {
    private final int statusCode;
    private final String endpoint;

    public GithubApiException(String message, int statusCode, String endpoint) {
        super(message);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
    }

    public GithubApiException(String message, int statusCode, String endpoint, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
    }
}
