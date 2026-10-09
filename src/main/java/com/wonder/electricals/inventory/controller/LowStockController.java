package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Dedicated page for rows at or below their reorder level. */
@Controller
@RequestMapping("/low-stock")
public class LowStockController {

    private final InventoryService inventoryService;

    public LowStockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", inventoryService.findLowStock());
        return "lowstock/list";
    }
}
