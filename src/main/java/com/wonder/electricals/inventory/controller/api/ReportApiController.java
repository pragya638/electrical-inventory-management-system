package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.PlantStockSummary;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** JSON equivalents of the six report screens. */
@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportService reportService;

    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory")
    public List<InventoryResponse> inventory() {
        return reportService.currentInventory();
    }

    @GetMapping("/plant-wise")
    public List<PlantStockSummary> plantWise() {
        return reportService.plantWiseInventory();
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> lowStock() {
        return reportService.lowStock();
    }

    @GetMapping("/stock-in")
    public List<StockTransactionResponse> stockIn(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.stockIn(from, to);
    }

    @GetMapping("/stock-out")
    public List<StockTransactionResponse> stockOut(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.stockOut(from, to);
    }

    @GetMapping("/transactions")
    public List<StockTransactionResponse> transactions(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.transactionHistory(from, to);
    }
}
