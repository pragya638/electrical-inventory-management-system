package com.wonder.electricals.inventory.dto.filter;

import com.wonder.electricals.inventory.dto.StockStatus;

/** Query parameters accepted by the inventory list page and {@code GET /api/inventory}. */
public class InventoryFilter {

    private Long productId;
    private Long plantId;
    private StockStatus status;

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

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }
}
