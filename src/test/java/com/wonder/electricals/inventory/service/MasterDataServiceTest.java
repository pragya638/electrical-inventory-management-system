package com.wonder.electricals.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wonder.electricals.inventory.dto.CategoryRequest;
import com.wonder.electricals.inventory.dto.SupplierRequest;
import com.wonder.electricals.inventory.dto.response.CategoryResponse;
import com.wonder.electricals.inventory.dto.response.SupplierResponse;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Supplier;
import com.wonder.electricals.inventory.exception.DuplicateResourceException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Category and supplier master-data rules (business rules 8 and 9). */
@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Spy
    private DtoMapper mapper = new DtoMapper();
    @InjectMocks
    private CategoryService categoryService;
    @InjectMocks
    private SupplierService supplierService;

    private CategoryRequest categoryRequest(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setDescription("Demo category");
        return request;
    }

    private SupplierRequest supplierRequest(String code) {
        SupplierRequest request = new SupplierRequest();
        request.setSupplierCode(code);
        request.setName("Demo Supplier");
        request.setEmail("orders@demo.example");
        request.setPhone("9001000001");
        return request;
    }

    // ---------------- Category ----------------

    @Test
    @DisplayName("creates a category")
    void createsCategory() {
        when(categoryRepository.existsByNameIgnoreCase("Cables")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CategoryResponse response = categoryService.create(categoryRequest("Cables"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Cables");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("rejects a duplicate category name (business rule 9)")
    void rejectsDuplicateCategoryName() {
        when(categoryRepository.existsByNameIgnoreCase("Cables")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(categoryRequest("Cables")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("category name");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("reports a missing category as not found")
    void reportsMissingCategory() {
        when(categoryRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(77L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("77");
    }

    @Test
    @DisplayName("filters categories by name or description")
    void filtersCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                TestFixtures.category(1L, "Cables"),
                TestFixtures.category(2L, "Switches")));

        assertThat(categoryService.findAll("cabl")).extracting(CategoryResponse::getName)
                .containsExactly("Cables");
        assertThat(categoryService.findAll("  ")).hasSize(2);
        assertThat(categoryService.findAll(null)).hasSize(2);
    }

    // ---------------- Supplier ----------------

    @Test
    @DisplayName("creates a supplier")
    void createsSupplier() {
        when(supplierRepository.existsBySupplierCodeIgnoreCase("SUP-001")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        SupplierResponse response = supplierService.create(supplierRequest("SUP-001"));

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getSupplierCode()).isEqualTo("SUP-001");
        assertThat(response.getName()).isEqualTo("Demo Supplier");
    }

    @Test
    @DisplayName("rejects a duplicate supplier code (business rule 8)")
    void rejectsDuplicateSupplierCode() {
        when(supplierRepository.existsBySupplierCodeIgnoreCase("SUP-001")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.create(supplierRequest("SUP-001")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("supplier code");

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    @DisplayName("rejects an update that would reuse another supplier's code")
    void rejectsDuplicateSupplierCodeOnUpdate() {
        Supplier existing = TestFixtures.supplier(5L, "SUP-001", "Existing");
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsBySupplierCodeIgnoreCaseAndIdNot("SUP-002", 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> supplierService.update(5L, supplierRequest("SUP-002")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("toggles supplier status")
    void togglesSupplierStatus() {
        Supplier existing = TestFixtures.supplier(5L, "SUP-001", "Existing");
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);

        SupplierResponse response = supplierService.changeStatus(5L, false);

        assertThat(response.isActive()).isFalse();
        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }
}