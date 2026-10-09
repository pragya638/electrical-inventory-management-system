package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The six report views plus the CSV download endpoints. */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String index(Model model) {
        return "reports/index";
    }

    @GetMapping("/inventory")
    public String currentInventory(Model model) {
        model.addAttribute("rows", reportService.currentInventory());
        model.addAttribute("reportTitle", "Current Inventory Report");
        return "reports/inventory";
    }

    @GetMapping("/plant-wise")
    public String plantWise(Model model) {
        model.addAttribute("rows", reportService.plantWiseInventory());
        model.addAttribute("reportTitle", "Plant-wise Inventory Report");
        return "reports/plant-wise";
    }

    @GetMapping("/stock-in")
    public String stockIn(@RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        model.addAttribute("rows", reportService.stockIn(from, to));
        model.addAttribute("reportTitle", "Stock IN Report");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "reports/transactions";
    }

    @GetMapping("/stock-out")
    public String stockOut(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           Model model) {
        model.addAttribute("rows", reportService.stockOut(from, to));
        model.addAttribute("reportTitle", "Stock OUT Report");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "reports/transactions";
    }

    @GetMapping("/low-stock")
    public String lowStock(Model model) {
        model.addAttribute("rows", reportService.lowStock());
        model.addAttribute("reportTitle", "Low Stock Report");
        return "reports/low-stock";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               Model model) {
        model.addAttribute("rows", reportService.transactionHistory(from, to));
        model.addAttribute("reportTitle", "Transaction History Report");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "reports/transactions";
    }

    @GetMapping("/export/inventory.csv")
    public ResponseEntity<String> exportInventoryCsv() {
        return csv("current-inventory.csv", reportService.currentInventoryAsCsv());
    }

    @GetMapping("/export/low-stock.csv")
    public ResponseEntity<String> exportLowStockCsv() {
        return csv("low-stock.csv", reportService.lowStockAsCsv());
    }

    @GetMapping("/export/transactions.csv")
    public ResponseEntity<String> exportTransactionsCsv(
            @RequestParam(required = false) String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        TransactionType direction = parseType(type);
        String fileName = direction == null ? "transactions.csv"
                : direction.name().toLowerCase(java.util.Locale.ROOT) + "-transactions.csv";
        return csv(fileName, reportService.transactionsAsCsv(direction, from, to));
    }

    private TransactionType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return TransactionType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ResponseEntity<String> csv(String fileName, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
