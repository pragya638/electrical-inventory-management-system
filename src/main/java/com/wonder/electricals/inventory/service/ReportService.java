package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.PlantStockSummary;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.util.CsvExporter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the six report views and the CSV export. Reports are plain queries over the
 * same services the pages use, so a report can never disagree with the screen.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final List<String> INVENTORY_HEADERS =
            List.of("Product Code", "Product", "Plant", "Quantity", "Unit", "Reorder Level", "Status",
                    "Last Updated");

    private static final List<String> TRANSACTION_HEADERS =
            List.of("Transaction ID", "Product Code", "Product", "Plant", "Type", "Quantity",
                    "Reference Number", "Remarks", "Date", "Performed By");

    private final InventoryService inventoryService;
    private final TransactionService transactionService;
    private final DashboardService dashboardService;
    private final CsvExporter csvExporter;

    public ReportService(InventoryService inventoryService,
                         TransactionService transactionService,
                         DashboardService dashboardService,
                         CsvExporter csvExporter) {
        this.inventoryService = inventoryService;
        this.transactionService = transactionService;
        this.dashboardService = dashboardService;
        this.csvExporter = csvExporter;
    }

    public List<InventoryResponse> currentInventory() {
        return inventoryService.findAll(null);
    }

    public List<PlantStockSummary> plantWiseInventory() {
        return dashboardService.buildPlantSummary();
    }

    public List<StockTransactionResponse> stockIn(LocalDate from, LocalDate to) {
        return transactionService.findForReport(TransactionType.IN, from, to);
    }

    public List<StockTransactionResponse> stockOut(LocalDate from, LocalDate to) {
        return transactionService.findForReport(TransactionType.OUT, from, to);
    }

    public List<InventoryResponse> lowStock() {
        return inventoryService.findLowStock();
    }

    public List<StockTransactionResponse> transactionHistory(LocalDate from, LocalDate to) {
        return transactionService.findForReport(null, from, to);
    }

    /** CSV export of the current inventory report. */
    public String currentInventoryAsCsv() {
        List<List<String>> rows = new ArrayList<>();
        for (InventoryResponse row : currentInventory()) {
            rows.add(List.of(
                    nullSafe(row.getProductCode()),
                    nullSafe(row.getProductName()),
                    nullSafe(row.getPlantName()),
                    String.valueOf(row.getQuantity()),
                    nullSafe(row.getUnit()),
                    String.valueOf(row.getReorderLevel()),
                    row.getStatus().name(),
                    nullSafe(String.valueOf(row.getLastUpdated()))));
        }
        return csvExporter.build(INVENTORY_HEADERS, rows);
    }

    /** CSV export of a stock movement report for either direction. */
    public String transactionsAsCsv(TransactionType type, LocalDate from, LocalDate to) {
        List<StockTransactionResponse> transactions = type == null
                ? transactionHistory(from, to)
                : transactionService.findForReport(type, from, to);

        List<List<String>> rows = new ArrayList<>();
        for (StockTransactionResponse row : transactions) {
            rows.add(List.of(
                    String.valueOf(row.getId()),
                    nullSafe(row.getProductCode()),
                    nullSafe(row.getProductName()),
                    nullSafe(row.getPlantName()),
                    row.getTransactionType().name(),
                    String.valueOf(row.getQuantity()),
                    nullSafe(row.getReferenceNumber()),
                    nullSafe(row.getRemarks()),
                    nullSafe(String.valueOf(row.getTransactionDate())),
                    nullSafe(row.getPerformedBy())));
        }
        return csvExporter.build(TRANSACTION_HEADERS, rows);
    }

    /** CSV export of the low stock report. */
    public String lowStockAsCsv() {
        List<List<String>> rows = new ArrayList<>();
        for (InventoryResponse row : lowStock()) {
            rows.add(List.of(
                    nullSafe(row.getProductCode()),
                    nullSafe(row.getProductName()),
                    nullSafe(row.getPlantName()),
                    String.valueOf(row.getQuantity()),
                    String.valueOf(row.getReorderLevel()),
                    String.valueOf(row.getReorderLevel() - row.getQuantity()),
                    row.getStatus().name(),
                    nullSafe(row.getUnit())));
        }
        return csvExporter.build(
                List.of("Product Code", "Product", "Plant", "Current Quantity", "Reorder Level",
                        "Shortfall", "Status", "Unit"),
                rows);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
