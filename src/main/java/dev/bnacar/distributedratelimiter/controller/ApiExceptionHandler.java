package dev.bnacar.distributedratelimiter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sanitizes uncaught API exceptions so clients do not receive internal details.
 */
@RestControllerAdvice(basePackages = "dev.bnacar.distributedratelimiter.controller")
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        BindException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        logger.warn("Rejected API request", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        logger.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("Internal server error"));
    }
}
