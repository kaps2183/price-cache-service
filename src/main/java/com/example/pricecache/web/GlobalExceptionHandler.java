package com.example.pricecache.web;

import com.example.pricecache.service.StalePriceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Translates validation and domain exceptions into {@link ApiError} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        log.warn("Rejected invalid price request: {}", details);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Rejected price violating a domain rule: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid price", List.of(ex.getMessage()));
    }

    @ExceptionHandler(StalePriceException.class)
    public ResponseEntity<ApiError> handleStalePrice(StalePriceException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Stale price", List.of(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Rejected unreadable request body: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", List.of(ex.getMostSpecificCause().getMessage()));
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String error, List<String> details) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), error, details));
    }
}
