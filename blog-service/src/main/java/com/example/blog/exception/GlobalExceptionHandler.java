package com.example.blog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(ConflictException ex) {
        String traceId = generateTraceId();
        log.warn("Conflict [traceId={}]: {}", traceId, ex.getMessage());
        // Return generic message, don't leak details about what already exists
        return buildErrorResponse("A conflict occurred with your request", HttpStatus.CONFLICT, traceId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        String traceId = generateTraceId();
        log.warn("Access denied [traceId={}]: {}", traceId, ex.getMessage());
        return buildErrorResponse("You do not have permission to perform this action", HttpStatus.FORBIDDEN, traceId);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        String traceId = generateTraceId();
        log.warn("Resource not found [traceId={}]: {}", traceId, ex.getMessage());
        // Return generic message without revealing which resource type
        return buildErrorResponse("The requested resource was not found", HttpStatus.NOT_FOUND, traceId);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        String traceId = generateTraceId();
        // Don't log the actual credentials, just the event
        log.warn("Authentication failed [traceId={}]", traceId);
        // Generic message - don't reveal if email exists or if password was wrong
        return buildErrorResponse("Authentication failed", HttpStatus.UNAUTHORIZED, traceId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = generateTraceId();
        log.warn("Invalid argument [traceId={}]: {}", traceId, ex.getMessage());
        // For validation errors, we can show the message as it's user input related
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, traceId);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String traceId = generateTraceId();
        // Log full details for debugging but don't expose to client
        log.error("Database conflict [traceId={}]: {}", traceId, ex.getMessage());
        return buildErrorResponse("A data conflict occurred. Please try again.", HttpStatus.CONFLICT, traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = generateTraceId();
        log.warn("Validation failed [traceId={}]: {}", traceId, ex.getBindingResult());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("details", errors);
        body.put("traceId", traceId);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String traceId = generateTraceId();
        // Log full stack trace for debugging
        log.error("Unexpected runtime exception [traceId={}]", traceId, ex);
        // Never expose internal details to client
        return buildErrorResponse("An unexpected error occurred. Reference: " + traceId, HttpStatus.INTERNAL_SERVER_ERROR, traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Unhandled exception [traceId={}]", traceId, ex);
        return buildErrorResponse("An unexpected error occurred. Reference: " + traceId, HttpStatus.INTERNAL_SERVER_ERROR, traceId);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status, String traceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("traceId", traceId);
        return new ResponseEntity<>(body, status);
    }
}