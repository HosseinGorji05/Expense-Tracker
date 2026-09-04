package com.hosseingorji.expensetracker.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Turns validation / bad-body failures into a small, stable JSON shape:
 * {@code {"status":400,"error":"...","errors":[{"field":"amount","message":"..."}]}}.
 * Keeps internal details (stack traces, class names) out of API responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toError)
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Validation failed",
                "errors", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Malformed or unreadable request body"));
    }

    private Map<String, String> toError(FieldError fe) {
        return Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage());
    }
}
