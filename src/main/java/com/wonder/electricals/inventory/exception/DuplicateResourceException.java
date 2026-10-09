package com.wonder.electricals.inventory.exception;

/**
 * Thrown when a uniqueness rule would be violated, for example a second product
 * with the same product code. Maps to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException of(String field, Object value) {
        return new DuplicateResourceException(
                "A record with " + field + " '" + value + "' already exists");
    }
}
