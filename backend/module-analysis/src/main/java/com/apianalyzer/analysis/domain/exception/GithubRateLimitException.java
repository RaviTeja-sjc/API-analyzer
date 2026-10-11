package com.apianalyzer.analysis.domain.exception;

import lombok.Getter;

@Getter
public class GithubRateLimitException extends RuntimeException {
    private final long resetEpochSeconds;
    private final int remaining;

    public GithubRateLimitException(String message, long resetEpochSeconds, int remaining) {
        super(message);
        this.resetEpochSeconds = resetEpochSeconds;
        this.remaining = remaining;
    }
}
