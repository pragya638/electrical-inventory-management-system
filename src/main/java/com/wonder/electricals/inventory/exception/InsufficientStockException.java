package com.wonder.electricals.inventory.exception;

/**
 * Business rule 3: a Stock OUT request larger than the available quantity is
 * rejected. Nothing is written, so inventory and history stay consistent.
 * Maps to HTTP 409.
 */
public class InsufficientStockException extends RuntimeException {

    private final int availableQuantity;
    private final int requestedQuantity;

    public InsufficientStockException(String productCode, String plantName,
                                      int availableQuantity, int requestedQuantity) {
        super("Insufficient stock for product " + productCode + " at " + plantName
                + ". Available: " + availableQuantity + ", requested: " + requestedQuantity);
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
