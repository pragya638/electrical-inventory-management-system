package com.wonder.electricals.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload shared by the Stock IN and Stock OUT endpoints. The direction of the
 * movement is decided by the endpoint the request is sent to, never by the client.
 */
public class StockMovementRequest {

    @NotNull(message = "Product is required")
    @Positive(message = "Please select a valid product")
    private Long productId;

    @NotNull(message = "Plant is required")
    @Positive(message = "Please select a valid plant")
    private Long plantId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @Size(max = 80, message = "Reference number must not exceed 80 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getPlantId() {
        return plantId;
    }

    public void setPlantId(Long plantId) {
        this.plantId = plantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
