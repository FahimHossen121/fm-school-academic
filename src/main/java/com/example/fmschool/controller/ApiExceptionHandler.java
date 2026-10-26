package com.example.fmschool.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {StudentController.class, TeacherApiController.class,
        SelfApiController.class, EnrollmentApiController.class})
public class ApiExceptionHandler {
    public record ErrorBody(int status, String message, Map<String, String> fields) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorBody(400, "Validation failed", fields));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorBody(ex.getStatusCode().value(),
                ex.getReason() == null ? "Request failed" : ex.getReason(), Map.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> denied() { return error(403, "Access denied"); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> conflict() {
        return error(409, "A unique value or course association conflicts with an existing record");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class})
    public ResponseEntity<ErrorBody> malformed() { return error(400, "Invalid request values or JSON"); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> invalidArgument(IllegalArgumentException ex) {
        return error(400, ex.getMessage());
    }

    private ResponseEntity<ErrorBody> error(int status, String message) {
        return ResponseEntity.status(status).body(new ErrorBody(status, message, Map.of()));
    }
}
