package com.wonder.electricals.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wonder.electricals.inventory.dto.ProductRequest;
import com.wonder.electricals.inventory.dto.filter.ProductFilter;
import com.wonder.electricals.inventory.dto.response.ProductResponse;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.exception.DuplicateResourceException;
import com.wonder.electricals.inventory.exception.OperationNotAllowedException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.StockTransactionRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for product creation, uniqueness rules and filtering. */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockTransactionRepository transactionRepository;

    private ProductService productService;

    private Category cables;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository,
                inventoryRepository, transactionRepository, new DtoMapper());
        cables = TestFixtures.category(1L, "Cables");
    }

    private ProductRequest validRequest() {
        ProductRequest request = new ProductRequest();
        request.setProductCode("CBL-100");
        request.setName("2.5 sq mm Copper Cable");
        request.setCategoryId(1L);
        request.setUnit("Meter");
        request.setReorderLevel(150);
        request.setDescription("Demo product");
        return request;
    }

    @Test
    @DisplayName("creates a product when the code is not already used")
    void createsProductSuccessfully() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cables));
        when(productRepository.existsByProductCodeIgnoreCase("CBL-100")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        ProductResponse response = productService.create(validRequest());

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getProductCode()).isEqualTo("CBL-100");
        assertThat(response.getCategoryName()).isEqualTo("Cables");
        assertThat(response.getReorderLevel()).isEqualTo(150);
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isSameAs(cables);
    }

    @Test
    @DisplayName("rejects a duplicate product code (business rule 7)")
    void rejectsDuplicateProductCode() {
        when(productRepository.existsByProductCodeIgnoreCase("CBL-100")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CBL-100");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("rejects an update that would reuse another product's code")
    void rejectsDuplicateCodeOnUpdate() {
        Product existing = TestFixtures.product(7L, "CBL-100", "Old name", cables, "Meter", 10);
        when(productRepository.findByIdWithCategory(7L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByProductCodeIgnoreCaseAndIdNot("CBL-200", 7L)).thenReturn(true);

        ProductRequest update = validRequest();
        update.setProductCode("CBL-200");

        assertThatThrownBy(() -> productService.update(7L, update))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("reports a missing product as ResourceNotFoundException")
    void reportsMissingProduct() {
        when(productRepository.findByIdWithCategory(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("rejects a create whose category does not exist")
    void rejectsUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        ProductRequest request = validRequest();
        request.setCategoryId(99L);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("deletes a product that has never moved stock")
    void deletesProductWithoutHistory() {
        Product existing = TestFixtures.product(7L, "CBL-100", "Name", cables, "Meter", 10);
        when(productRepository.findByIdWithCategory(7L)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsByProductId(7L)).thenReturn(false);
        when(inventoryRepository.findAllByProductId(7L)).thenReturn(List.of());

        productService.delete(7L);

        verify(productRepository).delete(existing);
    }

    @Test
    @DisplayName("refuses to delete a product that has transaction history")
    void refusesDeleteWhenHistoryExists() {
        Product existing = TestFixtures.product(7L, "CBL-100", "Name", cables, "Meter", 10);
        when(productRepository.findByIdWithCategory(7L)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsByProductId(7L)).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("Deactivate");

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("filters by search text, category and status")
    void filtersProducts() {
        Category switches = TestFixtures.category(2L, "Switches");
        Product cableA = TestFixtures.product(1L, "CBL-001", "Copper Cable", cables, "Meter", 10);
        Product switchA = TestFixtures.product(2L, "SWT-001", "Modular Switch", switches, "Piece", 5);
        switchA.setActive(false);
        when(productRepository.findAllWithCategory()).thenReturn(List.of(cableA, switchA));

        ProductFilter filter = new ProductFilter();
        filter.setSearch("switch");
        assertThat(productService.findAll(filter)).extracting(ProductResponse::getProductCode)
                .containsExactly("SWT-001");

        filter.setSearch(null);
        filter.setCategoryId(1L);
        assertThat(productService.findAll(filter)).extracting(ProductResponse::getProductCode)
                .containsExactly("CBL-001");

        filter.setCategoryId(null);
        filter.setStatus("active");
        assertThat(productService.findAll(filter)).extracting(ProductResponse::getProductCode)
                .containsExactly("CBL-001");

        filter.setStatus("inactive");
        assertThat(productService.findAll(filter)).extracting(ProductResponse::getProductCode)
                .containsExactly("SWT-001");
    }
}