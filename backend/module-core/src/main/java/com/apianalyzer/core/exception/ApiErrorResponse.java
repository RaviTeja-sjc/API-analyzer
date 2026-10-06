package com.apianalyzer.core.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ApiErrorResponse {
    private String traceId;       // Unique request ID for log tracing
    private String errorCode;     // Standardized enum string (e.g., "VALIDATION_FAILED")
    private String message;       // Safe, user-facing error message
    private Instant timestamp;    // ISO-8601 UTC timestamp
    private Map<String, String> validationDetails; // Field-level validation errors (if applicable)
}
