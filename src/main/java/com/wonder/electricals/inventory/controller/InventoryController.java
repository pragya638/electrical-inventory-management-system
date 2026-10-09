package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.filter.InventoryFilter;
import com.wonder.electricals.inventory.dto.response.PlantResponse;
import com.wonder.electricals.inventory.dto.response.ProductResponse;
import com.wonder.electricals.inventory.service.InventoryService;
import com.wonder.electricals.inventory.service.PlantService;
import com.wonder.electricals.inventory.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Current-stock page with product, plant and status filters. */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final PlantService plantService;

    public InventoryController(InventoryService inventoryService,
                               ProductService productService,
                               PlantService plantService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.plantService = plantService;
    }

    @GetMapping
    public String list(InventoryFilter filter, Model model) {
        model.addAttribute("rows", inventoryService.findAll(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("products", productService.findActive());
        model.addAttribute("plants", plantService.findActive());
        return "inventory/list";
    }
}
