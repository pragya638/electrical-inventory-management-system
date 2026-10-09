package com.wonder.electricals.inventory.dto.response;

import java.util.List;

/** Aggregated counters rendered as cards on the dashboard. */
public class DashboardSummary {

    private long totalProducts;
    private long totalCategories;
    private long totalPlants;
    private long totalSuppliers;
    private long currentStock;
    private long lowStockItems;
    private List<StockTransactionResponse> recentStockIn;
    private List<StockTransactionResponse> recentStockOut;
    private List<InventoryResponse> lowStockRows;
    private List<PlantStockSummary> plantStock;

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public long getTotalPlants() {
        return totalPlants;
    }

    public void setTotalPlants(long totalPlants) {
        this.totalPlants = totalPlants;
    }

    public long getTotalSuppliers() {
        return totalSuppliers;
    }

    public void setTotalSuppliers(long totalSuppliers) {
        this.totalSuppliers = totalSuppliers;
    }

    public long getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(long currentStock) {
        this.currentStock = currentStock;
    }

    public long getLowStockItems() {
        return lowStockItems;
    }

    public void setLowStockItems(long lowStockItems) {
        this.lowStockItems = lowStockItems;
    }

    public List<StockTransactionResponse> getRecentStockIn() {
        return recentStockIn;
    }

    public void setRecentStockIn(List<StockTransactionResponse> recentStockIn) {
        this.recentStockIn = recentStockIn;
    }

    public List<StockTransactionResponse> getRecentStockOut() {
        return recentStockOut;
    }

    public void setRecentStockOut(List<StockTransactionResponse> recentStockOut) {
        this.recentStockOut = recentStockOut;
    }

    public List<InventoryResponse> getLowStockRows() {
        return lowStockRows;
    }

    public void setLowStockRows(List<InventoryResponse> lowStockRows) {
        this.lowStockRows = lowStockRows;
    }

    public List<PlantStockSummary> getPlantStock() {
        return plantStock;
    }

    public void setPlantStock(List<PlantStockSummary> plantStock) {
        this.plantStock = plantStock;
    }
}
