package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.StockMovementRequest;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.service.StockService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stock movement endpoints. A Stock OUT that exceeds the available quantity is
 * rejected with HTTP 409 by {@code InsufficientStockException}.
 */
@RestController
@RequestMapping("/api/stock")
public class StockApiController {

    private final StockService stockService;

    public StockApiController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/in")
    public ResponseEntity<StockTransactionResponse> stockIn(
            @Valid @RequestBody StockMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.stockIn(request));
    }

    @PostMapping("/out")
    public ResponseEntity<StockTransactionResponse> stockOut(
            @Valid @RequestBody StockMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.stockOut(request));
    }

    /** Convenience read used to confirm the quantity after a movement. */
    @GetMapping("/current")
    public ResponseEntity<InventoryResponse> currentStock(@RequestParam Long productId,
                                                         @RequestParam Long plantId) {
        return ResponseEntity.of(Optional.ofNullable(stockService.currentStock(productId, plantId)));
    }
}
