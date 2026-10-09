package com.wonder.electricals.inventory.exception;

/**
 * Thrown when an operation is refused for a business reason that is not a
 * validation or duplicate problem, e.g. deleting a product that already has
 * stock transactions. Maps to HTTP 409.
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
