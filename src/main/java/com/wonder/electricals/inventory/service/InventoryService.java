package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.StockStatus;
import com.wonder.electricals.inventory.dto.filter.InventoryFilter;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only view over current stock. Quantity is never written here: stock only moves
 * through {@link StockService}.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final DtoMapper mapper;

    public InventoryService(InventoryRepository inventoryRepository, DtoMapper mapper) {
        this.inventoryRepository = inventoryRepository;
        this.mapper = mapper;
    }

    public List<InventoryResponse> findAll(InventoryFilter filter) {
        Long productId = filter == null ? null : filter.getProductId();
        Long plantId = filter == null ? null : filter.getPlantId();
        StockStatus status = filter == null ? null : filter.getStatus();
        return mapper.toInventoryResponses(inventoryRepository.search(productId, plantId,
                status == null ? null : status.name()));
    }

    public InventoryResponse findById(Long id) {
        return mapper.toInventoryResponse(inventoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventory record", id)));
    }

    public List<InventoryResponse> findByProduct(Long productId) {
        return mapper.toInventoryResponses(inventoryRepository.findByProductId(productId));
    }

    public List<InventoryResponse> findByPlant(Long plantId) {
        return mapper.toInventoryResponses(inventoryRepository.findByPlantId(plantId));
    }

    /** Business rule 6: {@code quantity <= reorderLevel}, ordered worst-first. */
    public List<InventoryResponse> findLowStock() {
        return mapper.toInventoryResponses(inventoryRepository.findLowStock());
    }

    public long currentStockTotal() {
        return inventoryRepository.totalQuantity();
    }
}
