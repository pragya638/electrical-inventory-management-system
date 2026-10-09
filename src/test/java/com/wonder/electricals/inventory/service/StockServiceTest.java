package com.wonder.electricals.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wonder.electricals.inventory.dto.StockMovementRequest;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.entity.StockTransaction;
import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.exception.InsufficientStockException;
import com.wonder.electricals.inventory.exception.OperationNotAllowedException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.StockTransactionRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Covers business rules 1 to 5: stock IN, stock OUT, insufficient stock, no negative stock. */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PlantRepository plantRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockTransactionRepository transactionRepository;

    private StockService stockService;

    private Category cables;
    private Product cable;
    private Plant plant;

    @BeforeEach
    void setUp() {
        stockService = new StockService(productRepository, plantRepository, inventoryRepository,
                transactionRepository, new DtoMapper());
        cables = TestFixtures.category(1L, "Cables");
        cable = TestFixtures.product(1L, "CBL-001", "Copper Cable", cables, "Meter", 200);
        plant = TestFixtures.plant(1L, "Plant A (DEMO)");
    }

    private void stubProductAndPlant() {
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(cable));
        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));
    }

    private void stubSave() {
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(invocation -> {
            StockTransaction saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("rule 1: Stock IN increases the quantity and writes a transaction")
    void stockInIncreasesQuantity() {
        stubProductAndPlant();
        Inventory existing = TestFixtures.inventory(5L, cable, plant, 100);
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L)).thenReturn(Optional.of(existing));
        stubSave();

        StockTransactionResponse response = stockService.stockIn(
                TestFixtures.movement(1L, 1L, 20));

        assertThat(response.getTransactionType()).isEqualTo(TransactionType.IN);
        assertThat(response.getQuantity()).isEqualTo(20);
        assertThat(existing.getQuantity()).isEqualTo(120);

        ArgumentCaptor<StockTransaction> captor = ArgumentCaptor.forClass(StockTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionType()).isEqualTo(TransactionType.IN);
        assertThat(captor.getValue().getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("rule 1: Stock IN creates the inventory row when the product is new at that plant")
    void stockInCreatesInventoryRowWhenMissing() {
        stubProductAndPlant();
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L)).thenReturn(Optional.empty());
        stubSave();

        stockService.stockIn(TestFixtures.movement(1L, 1L, 40));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(40);
        assertThat(captor.getValue().getProduct()).isSameAs(cable);
        assertThat(captor.getValue().getPlant()).isSameAs(plant);
    }

    @Test
    @DisplayName("rule 2: Stock OUT decreases the quantity and writes a transaction")
    void stockOutDecreasesQuantity() {
        stubProductAndPlant();
        Inventory existing = TestFixtures.inventory(5L, cable, plant, 100);
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L)).thenReturn(Optional.of(existing));
        stubSave();

        stockService.stockOut(TestFixtures.movement(1L, 1L, 30));

        assertThat(existing.getQuantity()).isEqualTo(70);

        ArgumentCaptor<StockTransaction> captor = ArgumentCaptor.forClass(StockTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionType()).isEqualTo(TransactionType.OUT);
        assertThat(captor.getValue().getQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("rule 3: Stock OUT larger than available is rejected and nothing is written")
    void rejectsInsufficientStock() {
        stubProductAndPlant();
        Inventory existing = TestFixtures.inventory(5L, cable, plant, 20);
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> stockService.stockOut(TestFixtures.movement(1L, 1L, 25)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 20")
                .hasMessageContaining("requested: 25");

        // Rules 3 and 4: inventory untouched and no transaction created.
        assertThat(existing.getQuantity()).isEqualTo(20);
        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(transactionRepository, never()).save(any(StockTransaction.class));
    }

    @Test
    @DisplayName("rule 4: quantity can reach exactly zero but never go below")
    void allowsDrainingToZeroButNotBelow() {
        stubProductAndPlant();
        Inventory existing = TestFixtures.inventory(5L, cable, plant, 20);
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L)).thenReturn(Optional.of(existing));
        stubSave();

        stockService.stockOut(TestFixtures.movement(1L, 1L, 20));
        assertThat(existing.getQuantity()).isZero();

        assertThatThrownBy(() -> stockService.stockOut(TestFixtures.movement(1L, 1L, 1)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(existing.getQuantity()).isZero();
    }

    @Test
    @DisplayName("rule 3: Stock OUT on a product that was never stocked is rejected")
    void rejectsStockOutWithNoInventoryRow() {
        stubProductAndPlant();
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.stockOut(TestFixtures.movement(1L, 1L, 5)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 0");

        verify(transactionRepository, never()).save(any(StockTransaction.class));
    }

    @Test
    @DisplayName("rejects a non-positive quantity even if validation is bypassed")
    void rejectsNonPositiveQuantity() {
        stubProductAndPlant();
        StockMovementRequest request = TestFixtures.movement(1L, 1L, 5);
        ReflectionTestUtils.setField(request, "quantity", 0);

        assertThatThrownBy(() -> stockService.stockIn(request))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("greater than zero");

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("reports unknown product and plant as not found")
    void reportsMissingReferences() {
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> stockService.stockIn(TestFixtures.movement(99L, 1L, 5)))
                .isInstanceOf(ResourceNotFoundException.class);

        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(cable));
        when(plantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> stockService.stockIn(TestFixtures.movement(1L, 99L, 5)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("refuses to move stock for a deactivated product or plant")
    void refusesInactiveReferences() {
        cable.setActive(false);
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(cable));

        assertThatThrownBy(() -> stockService.stockIn(TestFixtures.movement(1L, 1L, 5)))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("deactivated");

        cable.setActive(true);
        plant.setActive(false);
        when(plantRepository.findById(1L)).thenReturn(Optional.of(plant));

        assertThatThrownBy(() -> stockService.stockIn(TestFixtures.movement(1L, 1L, 5)))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("writes the performed-by value from the security context")
    void recordsPerformedBy() {
        stubProductAndPlant();
        when(inventoryRepository.findByProductIdAndPlantId(1L, 1L))
                .thenReturn(Optional.of(TestFixtures.inventory(5L, cable, plant, 10)));
        stubSave();

        StockTransactionResponse response = stockService.stockIn(
                TestFixtures.movement(1L, 1L, 5));

        // No authentication context in a unit test, so the fallback value is used.
        assertThat(response.getPerformedBy()).isEqualTo("system");
        assertThat(response.getProductCode()).isEqualTo("CBL-001");
        assertThat(response.getPlantName()).isEqualTo("Plant A (DEMO)");
    }
}