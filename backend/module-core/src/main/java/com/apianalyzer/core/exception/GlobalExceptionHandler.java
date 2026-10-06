package com.apianalyzer.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        // Server-side logging: Log the full stack trace and original message
        log.error("Unhandled exception occurred. TraceID: {}", traceId, ex);

        // Client-side response: Completely sanitized. Never expose stack trace or internal details.
        ApiErrorResponse response = ApiErrorResponse.builder()
                .traceId(traceId)
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please contact support and reference the trace ID.")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurityException(SecurityException ex) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Security/Authentication failure. TraceID: {} Message: {}", traceId, ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .traceId(traceId)
                .errorCode("UNAUTHORIZED")
                .message("Authentication failed or insufficient permissions.")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Illegal argument provided. TraceID: {} Message: {}", traceId, ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .traceId(traceId)
                .errorCode("BAD_REQUEST")
                .message(ex.getMessage()) // IllegalArgument usually contains safe validation messages
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = UUID.randomUUID().toString();
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed. TraceID: {} Details: {}", traceId, errors);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .traceId(traceId)
                .errorCode("VALIDATION_FAILED")
                .message("One or more request parameters failed validation.")
                .validationDetails(errors)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
