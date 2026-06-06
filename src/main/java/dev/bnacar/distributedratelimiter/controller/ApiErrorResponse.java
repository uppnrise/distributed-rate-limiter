package dev.bnacar.distributedratelimiter.controller;

/**
 * Minimal error response for client-facing API failures.
 */
public record ApiErrorResponse(String error) {
}
