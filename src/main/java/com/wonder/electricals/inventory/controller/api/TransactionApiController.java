package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.filter.TransactionFilter;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.service.TransactionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionApiController {

    private final TransactionService transactionService;

    public TransactionApiController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** Supports ?type=IN, ?productId=, ?plantId=, ?from=, ?to=, ?page=, ?size=. */
    @GetMapping
    public List<StockTransactionResponse> list(@ModelAttribute TransactionFilter filter) {
        return transactionService.search(filter);
    }

    @GetMapping("/{id}")
    public StockTransactionResponse get(@PathVariable Long id) {
        return transactionService.findById(id);
    }
}
