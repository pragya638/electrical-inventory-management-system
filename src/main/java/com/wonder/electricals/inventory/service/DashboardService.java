package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.response.DashboardSummary;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.PlantStockSummary;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles the numbers rendered on the landing page. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_LIMIT = 8;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PlantRepository plantRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final TransactionService transactionService;
    private final DtoMapper mapper;

    public DashboardService(ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            PlantRepository plantRepository,
                            SupplierRepository supplierRepository,
                            InventoryRepository inventoryRepository,
                            TransactionService transactionService,
                            DtoMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.plantRepository = plantRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionService = transactionService;
        this.mapper = mapper;
    }

    public DashboardSummary buildSummary() {
        List<InventoryResponse> lowStock = mapper.toInventoryResponses(inventoryRepository.findLowStock());

        DashboardSummary summary = new DashboardSummary();
        summary.setTotalProducts(productRepository.countByActiveTrue());
        summary.setTotalCategories(categoryRepository.countByActiveTrue());
        summary.setTotalPlants(plantRepository.countByActiveTrue());
        summary.setTotalSuppliers(supplierRepository.countByActiveTrue());
        summary.setCurrentStock(inventoryRepository.totalQuantity());
        summary.setLowStockItems(lowStock.size());
        summary.setRecentStockIn(transactionService.findRecent(TransactionType.IN, RECENT_LIMIT));
        summary.setRecentStockOut(transactionService.findRecent(TransactionType.OUT, RECENT_LIMIT));
        summary.setLowStockRows(lowStock);
        summary.setPlantStock(buildPlantSummary());
        return summary;
    }

    /** Totals per plant, including how many of that plant's rows are below reorder level. */
    public List<PlantStockSummary> buildPlantSummary() {
        List<Plant> plants = plantRepository.findAllByOrderByNameAsc();
        List<Inventory> inventory = inventoryRepository.findAllDetailed();

        List<PlantStockSummary> summaries = new ArrayList<>();
        for (Plant plant : plants) {
            List<Inventory> rows = inventory.stream()
                    .filter(row -> row.getPlant().getId().equals(plant.getId()))
                    .toList();

            PlantStockSummary plantSummary = new PlantStockSummary();
            plantSummary.setPlantId(plant.getId());
            plantSummary.setPlantName(plant.getName());
            plantSummary.setLocation(plant.getLocation());
            plantSummary.setTotalQuantity(rows.stream().mapToLong(Inventory::getQuantity).sum());
            plantSummary.setProductCount(rows.size());
            plantSummary.setLowStockCount((long) rows.stream()
                    .filter(row -> row.getQuantity() <= row.getReorderLevel())
                    .count());
            summaries.add(plantSummary);
        }
        return summaries;
    }

    public List<StockTransactionResponse> recentStockIn() {
        return transactionService.findRecent(TransactionType.IN, RECENT_LIMIT);
    }

    public List<StockTransactionResponse> recentStockOut() {
        return transactionService.findRecent(TransactionType.OUT, RECENT_LIMIT);
    }
}
