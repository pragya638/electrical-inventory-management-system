package com.wonder.electricals.inventory.service;

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
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product master data. Enforces the unique product code rule (business rule 7) and
 * refuses hard deletion of a product that already has movement history.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository transactionRepository;
    private final DtoMapper mapper;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          InventoryRepository inventoryRepository,
                          StockTransactionRepository transactionRepository,
                          DtoMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    public List<ProductResponse> findAll(ProductFilter filter) {
        return mapper.toProductResponses(applyFilter(productRepository.findAllWithCategory(), filter));
    }

    public List<ProductResponse> findActive() {
        return mapper.toProductResponses(productRepository.findAllActiveWithCategory());
    }

    public ProductResponse findById(Long id) {
        return mapper.toProductResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String code = request.getProductCode().trim();
        if (productRepository.existsByProductCodeIgnoreCase(code)) {
            throw DuplicateResourceException.of("product code", code);
        }
        Product product = new Product();
        applyRequest(product, request);
        product.setProductCode(code);
        return mapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        String code = request.getProductCode().trim();
        if (productRepository.existsByProductCodeIgnoreCaseAndIdNot(code, id)) {
            throw DuplicateResourceException.of("product code", code);
        }
        product.setProductCode(code);
        applyRequest(product, request);
        return mapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse changeStatus(Long id, boolean active) {
        Product product = getEntity(id);
        product.setActive(active);
        return mapper.toProductResponse(productRepository.save(product));
    }

    /**
     * Hard delete is only safe for a product that has never moved stock. Anything with
     * history is deactivated instead so the transaction log stays complete.
     */
    @Transactional
    public void delete(Long id) {
        Product product = getEntity(id);
        boolean hasHistory = transactionRepository.existsByProductId(id)
                || !inventoryRepository.findAllByProductId(id).isEmpty();
        if (hasHistory) {
            throw new OperationNotAllowedException(
                    "Product " + product.getProductCode() + " has stock or transaction history. "
                            + "Deactivate it instead of deleting.");
        }
        productRepository.delete(product);
    }

    private void applyRequest(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.getCategoryId()));
        product.setName(request.getName().trim());
        product.setCategory(category);
        product.setUnit(request.getUnit().trim());
        product.setReorderLevel(request.getReorderLevel());
        product.setDescription(request.getDescription());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
    }

    private List<Product> applyFilter(List<Product> products, ProductFilter filter) {
        if (filter == null) {
            return products;
        }
        String search = filter.getSearch();
        String needle = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
        Long categoryId = filter.getCategoryId();
        String status = filter.getStatus();

        return products.stream()
                .filter(product -> needle == null
                        || needle.isEmpty()
                        || product.getName().toLowerCase(Locale.ROOT).contains(needle)
                        || product.getProductCode().toLowerCase(Locale.ROOT).contains(needle)
                        || product.getCategory().getName().toLowerCase(Locale.ROOT).contains(needle))
                .filter(product -> categoryId == null || product.getCategory().getId().equals(categoryId))
                .filter(product -> status == null || status.isBlank()
                        || ("active".equalsIgnoreCase(status) && product.isActive())
                        || ("inactive".equalsIgnoreCase(status) && !product.isActive()))
                .toList();
    }

    private Product getEntity(Long id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }
}
