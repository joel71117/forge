package com.forge.common.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest webRequest) {
        var body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", ex.getMessage(),
                webRequest.getDescription(false), null, List.of());
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest webRequest) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), webRequest, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest webRequest) {
        return response(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), webRequest, List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleStateConflict(IllegalStateException ex, WebRequest webRequest) {
        return response(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), webRequest, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest webRequest) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        var body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Validation failed",
                webRequest.getDescription(false), null, details);
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, WebRequest webRequest) {
        var body = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "An unexpected error occurred", webRequest.getDescription(false), null, List.of());
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message, WebRequest webRequest,
            List<String> details) {
        return new ResponseEntity<>(
                new ErrorResponse(status.value(), code, message, webRequest.getDescription(false), null, details),
                new HttpHeaders(), status);
    }
}
