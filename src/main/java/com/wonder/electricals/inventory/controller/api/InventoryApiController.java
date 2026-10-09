package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.filter.InventoryFilter;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.service.InventoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only REST view of current stock. */
@RestController
@RequestMapping("/api/inventory")
public class InventoryApiController {

    private final InventoryService inventoryService;

    public InventoryApiController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> list(InventoryFilter filter) {
        return inventoryService.findAll(filter);
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> lowStock() {
        return inventoryService.findLowStock();
    }

    @GetMapping("/product/{productId}")
    public List<InventoryResponse> byProduct(@PathVariable Long productId) {
        return inventoryService.findByProduct(productId);
    }

    @GetMapping("/plant/{plantId}")
    public List<InventoryResponse> byPlant(@PathVariable Long plantId) {
        return inventoryService.findByPlant(plantId);
    }

    @GetMapping("/{id}")
    public InventoryResponse get(@PathVariable Long id) {
        return inventoryService.findById(id);
    }
}
