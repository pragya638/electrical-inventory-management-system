package com.wonder.electricals.inventory.dto;

import com.wonder.electricals.inventory.entity.Inventory;

/** Stock health of an inventory row, derived in the service layer. */
public enum StockStatus {

    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK;

    /**
     * Business rule 6: low stock is {@code quantity <= reorderLevel}.
     * A quantity of zero is reported separately as OUT OF STOCK so the UI can
     * distinguish "running out" from "already empty".
     */
    public static StockStatus of(Inventory inventory) {
        if (inventory.getQuantity() <= 0) {
            return OUT_OF_STOCK;
        }
        return inventory.getQuantity() <= inventory.getReorderLevel() ? LOW_STOCK : IN_STOCK;
    }
}

