package com.everest.locker.controller;

import com.everest.locker.entity.Size;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.LockerException;
import com.everest.locker.exception.NoSuitableLockerException;
import com.everest.locker.exception.NotificationFailedException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 {@code ProblemDetail} with a stable {@code code}
 * property that clients can switch on. Internal details never reach the client.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(LockerException.class)
    ProblemDetail handleLockerException(LockerException ex) {
        // Exhaustive over the sealed hierarchy: a new exception type will not compile until mapped here.
        return switch (ex) {
            case InvalidRequestException e -> problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
            case NoSuitableLockerException e -> problem(HttpStatus.CONFLICT, "NO_SUITABLE_LOCKER", e.getMessage());
            case NotificationFailedException e -> {
                log.error("Customer notification failed", e);
                yield problem(HttpStatus.SERVICE_UNAVAILABLE, "NOTIFICATION_FAILED", e.getMessage());
            }
        };
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detail));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Request body is not valid JSON or has a wrong value. Sizes must be one of "
                + Arrays.stream(Size.values()).map(Enum::name).collect(Collectors.joining(", ")) + ".";
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", detail));
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
