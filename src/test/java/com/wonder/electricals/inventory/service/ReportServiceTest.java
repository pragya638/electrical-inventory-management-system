package com.wonder.electricals.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wonder.electricals.inventory.dto.StockStatus;
import com.wonder.electricals.inventory.dto.filter.InventoryFilter;
import com.wonder.electricals.inventory.dto.response.DashboardSummary;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.PlantStockSummary;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Low stock calculation (business rule 6) and dashboard aggregation. */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PlantRepository plantRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private TransactionService transactionService;

    @Spy
    private DtoMapper mapper = new DtoMapper();

    @InjectMocks
    private InventoryService inventoryService;
    @InjectMocks
    private DashboardService dashboardService;

    private Product cableWithReorderLevel(int reorderLevel) {
        return TestFixtures.product(1L, "CBL-001", "Copper Cable",
                TestFixtures.category(1L, "Cables"), "Meter", reorderLevel);
    }

    @Test
    @DisplayName("rule 6: quantity equal to the reorder level is LOW STOCK")
    void quantityEqualToReorderLevelIsLowStock() {
        Inventory inventory = TestFixtures.inventory(1L, cableWithReorderLevel(200),
                TestFixtures.plant(1L, "Plant A"), 200);

        assertThat(StockStatus.of(inventory)).isEqualTo(StockStatus.LOW_STOCK);
    }

    @Test
    @DisplayName("quantity below the reorder level is LOW STOCK")
    void quantityBelowReorderLevelIsLowStock() {
        Inventory inventory = TestFixtures.inventory(1L, cableWithReorderLevel(200),
                TestFixtures.plant(1L, "Plant A"), 20);

        assertThat(StockStatus.of(inventory)).isEqualTo(StockStatus.LOW_STOCK);
    }

    @Test
    @DisplayName("quantity above the reorder level is IN STOCK")
    void quantityAboveReorderLevelIsInStock() {
        Inventory inventory = TestFixtures.inventory(1L, cableWithReorderLevel(200),
                TestFixtures.plant(1L, "Plant A"), 201);

        assertThat(StockStatus.of(inventory)).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("quantity of zero is reported separately as OUT OF STOCK")
    void zeroQuantityIsOutOfStock() {
        Inventory inventory = TestFixtures.inventory(1L, cableWithReorderLevel(200),
                TestFixtures.plant(1L, "Plant A"), 0);

        assertThat(StockStatus.of(inventory)).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("low stock list is delegated to the repository query")
    void lowStockUsesRepositoryQuery() {
        Inventory inventory = TestFixtures.inventory(1L, cableWithReorderLevel(200),
                TestFixtures.plant(1L, "Plant A"), 20);
        when(inventoryRepository.findLowStock()).thenReturn(List.of(inventory));

        List<InventoryResponse> rows = inventoryService.findLowStock();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo(StockStatus.LOW_STOCK);
        assertThat(rows.get(0).getQuantity()).isEqualTo(20);
        assertThat(rows.get(0).getReorderLevel()).isEqualTo(200);
        verify(inventoryRepository).findLowStock();
    }

    @Test
    @DisplayName("inventory filter passes the status name through to the query")
    void inventoryFilterUsesStatusName() {
        when(inventoryRepository.search(1L, 2L, "LOW_STOCK")).thenReturn(List.of());

        InventoryFilter filter = new InventoryFilter();
        filter.setProductId(1L);
        filter.setPlantId(2L);
        filter.setStatus(StockStatus.LOW_STOCK);

        inventoryService.findAll(filter);

        verify(inventoryRepository).search(1L, 2L, "LOW_STOCK");
    }

    @Test
    @DisplayName("an absent filter searches on every column being null")
    void absentFilterSearchesEverything() {
        when(inventoryRepository.search(null, null, null)).thenReturn(List.of());

        inventoryService.findAll(new InventoryFilter());

        verify(inventoryRepository).search(null, null, null);
    }

    @Test
    @DisplayName("reports a missing inventory row as not found")
    void reportsMissingInventoryRow() {
        when(inventoryRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.findById(77L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("dashboard summary aggregates counts, totals and per-plant rows")
    void dashboardSummaryAggregatesCounts() {
        Plant plantA = TestFixtures.plant(1L, "Plant A");
        Plant plantB = TestFixtures.plant(2L, "Plant B");
        Product cable = cableWithReorderLevel(200);
        Inventory lowRow = TestFixtures.inventory(1L, cable, plantA, 50);
        Inventory healthyRow = TestFixtures.inventory(2L, TestFixtures.product(2L, "SWT-001", "Switch",
                TestFixtures.category(2L, "Switches"), "Piece", 10), plantB, 400);

        when(productRepository.countByActiveTrue()).thenReturn(12L);
        when(categoryRepository.countByActiveTrue()).thenReturn(5L);
        when(plantRepository.countByActiveTrue()).thenReturn(3L);
        when(supplierRepository.countByActiveTrue()).thenReturn(3L);
        when(inventoryRepository.findLowStock()).thenReturn(List.of(lowRow));
        when(inventoryRepository.totalQuantity()).thenReturn(450L);
        when(plantRepository.findAllByOrderByNameAsc()).thenReturn(List.of(plantA, plantB));
        when(inventoryRepository.findAllDetailed()).thenReturn(List.of(lowRow, healthyRow));
        when(transactionService.findRecent(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());

        DashboardSummary summary = dashboardService.buildSummary();

        assertThat(summary.getTotalProducts()).isEqualTo(12L);
        assertThat(summary.getTotalCategories()).isEqualTo(5L);
        assertThat(summary.getTotalPlants()).isEqualTo(3L);
        assertThat(summary.getTotalSuppliers()).isEqualTo(3L);
        assertThat(summary.getCurrentStock()).isEqualTo(450L);
        assertThat(summary.getLowStockItems()).isEqualTo(1);

        assertThat(summary.getPlantStock()).hasSize(2);
        assertThat(summary.getPlantStock()).extracting(PlantStockSummary::getPlantName)
                .containsExactly("Plant A", "Plant B");

        PlantStockSummary plantASummary = summary.getPlantStock().get(0);
        assertThat(plantASummary.getTotalQuantity()).isEqualTo(50L);
        assertThat(plantASummary.getProductCount()).isEqualTo(1L);
        assertThat(plantASummary.getLowStockCount()).isEqualTo(1L);

        PlantStockSummary plantBSummary = summary.getPlantStock().get(1);
        assertThat(plantBSummary.getTotalQuantity()).isEqualTo(400L);
        assertThat(plantBSummary.getLowStockCount()).isZero();
    }
}