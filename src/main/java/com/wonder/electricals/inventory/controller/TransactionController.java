package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.filter.TransactionFilter;
import com.wonder.electricals.inventory.service.PlantService;
import com.wonder.electricals.inventory.service.ProductService;
import com.wonder.electricals.inventory.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/** Transaction history page with IN/OUT, product, plant and date filters. */
@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final ProductService productService;
    private final PlantService plantService;

    public TransactionController(TransactionService transactionService,
                                 ProductService productService,
                                 PlantService plantService) {
        this.transactionService = transactionService;
        this.productService = productService;
        this.plantService = plantService;
    }

    @GetMapping
    public String list(@ModelAttribute("filter") TransactionFilter filter, Model model) {
        model.addAttribute("transactions", transactionService.search(filter));
        model.addAttribute("products", productService.findActive());
        model.addAttribute("plants", plantService.findActive());
        return "transactions/list";
    }
}
