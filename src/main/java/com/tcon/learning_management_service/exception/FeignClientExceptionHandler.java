package com.tcon.learning_management_service.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class FeignClientExceptionHandler {

    // ─────────────────────────────────────────────
    // GENERIC FEIGN ERROR
    // ─────────────────────────────────────────────
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeignException(
            FeignException e
    ) {
        log.error("❌ Feign client error: status={}, message={}",
                e.status(), e.getMessage());

        HttpStatus status = resolveStatus(e.status());

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "error", "External service error",
                        "message", e.getMessage(),
                        "service", "video-service"
                ));
    }

    // ─────────────────────────────────────────────
    // 404 — Session not found in video-service
    // ─────────────────────────────────────────────
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String, String>> handleFeignNotFound(
            FeignException.NotFound e
    ) {
        log.warn("⚠️ Feign 404: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Resource not found in external service",
                        "message", e.getMessage()
                ));
    }

    // ─────────────────────────────────────────────
    // 503 — video-service is down
    // ─────────────────────────────────────────────
    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<Map<String, String>> handleFeignServiceUnavailable(
            FeignException.ServiceUnavailable e
    ) {
        log.error("❌ Feign service unavailable: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Video service is currently unavailable",
                        "message", "Please try again later"
                ));
    }

    // ─────────────────────────────────────────────
    // GENERAL RUNTIME ERROR
    // ─────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException e
    ) {
        log.error("❌ Runtime error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage()
                ));
    }

    // ─────────────────────────────────────────────
    // ILLEGAL ARGUMENT (validation errors)
    // ─────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException e
    ) {
        log.warn("⚠️ Validation error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation failed",
                        "message", e.getMessage()
                ));
    }

    // ─────────────────────────────────────────────
    // PRIVATE: Resolve HTTP status from Feign error code
    // ─────────────────────────────────────────────
    private HttpStatus resolveStatus(int feignStatus) {
        return switch (feignStatus) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default  -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}