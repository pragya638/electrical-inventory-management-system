package com.wonder.electricals.inventory.dto;

import java.time.Instant;

/**
 * Structured error body returned by the REST layer. Deliberately carries no
 * stack trace so internal details are not leaked to API consumers.
 */
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String[] fieldErrors;

    public ApiErrorResponse(Instant timestamp, int status, String error, String message,
                            String path, String[] fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, new String[0]);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path,
                                      String[] fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String[] getFieldErrors() {
        return fieldErrors;
    }
}
