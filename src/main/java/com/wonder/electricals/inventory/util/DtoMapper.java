package com.wonder.electricals.inventory.util;

import com.wonder.electricals.inventory.dto.StockStatus;
import com.wonder.electricals.inventory.dto.response.CategoryResponse;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.PlantResponse;
import com.wonder.electricals.inventory.dto.response.ProductResponse;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.dto.response.SupplierResponse;
import com.wonder.electricals.inventory.dto.response.UserResponse;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.entity.StockTransaction;
import com.wonder.electricals.inventory.entity.Supplier;
import com.wonder.electricals.inventory.entity.User;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converts entities to response DTOs. Doing this inside the service layer (while the
 * transaction is still open) is what keeps LAZY associations from causing a
 * LazyInitializationException, and it also stops controllers from touching entities
 * directly.
 */
@Component
public class DtoMapper {

    public ProductResponse toProductResponse(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setName(product.getName());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setUnit(product.getUnit());
        dto.setReorderLevel(product.getReorderLevel());
        dto.setDescription(product.getDescription());
        dto.setActive(product.isActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    public List<ProductResponse> toProductResponses(List<Product> products) {
        return products.stream().map(this::toProductResponse).toList();
    }

    public CategoryResponse toCategoryResponse(Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setActive(category.isActive());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    public List<CategoryResponse> toCategoryResponses(List<Category> categories) {
        return categories.stream().map(this::toCategoryResponse).toList();
    }

    public PlantResponse toPlantResponse(Plant plant) {
        PlantResponse dto = new PlantResponse();
        dto.setId(plant.getId());
        dto.setName(plant.getName());
        dto.setLocation(plant.getLocation());
        dto.setContactPerson(plant.getContactPerson());
        dto.setContactNumber(plant.getContactNumber());
        dto.setActive(plant.isActive());
        dto.setCreatedAt(plant.getCreatedAt());
        dto.setUpdatedAt(plant.getUpdatedAt());
        return dto;
    }

    public List<PlantResponse> toPlantResponses(List<Plant> plants) {
        return plants.stream().map(this::toPlantResponse).toList();
    }

    public SupplierResponse toSupplierResponse(Supplier supplier) {
        SupplierResponse dto = new SupplierResponse();
        dto.setId(supplier.getId());
        dto.setSupplierCode(supplier.getSupplierCode());
        dto.setName(supplier.getName());
        dto.setContactPerson(supplier.getContactPerson());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setActive(supplier.isActive());
        dto.setCreatedAt(supplier.getCreatedAt());
        dto.setUpdatedAt(supplier.getUpdatedAt());
        return dto;
    }

    public List<SupplierResponse> toSupplierResponses(List<Supplier> suppliers) {
        return suppliers.stream().map(this::toSupplierResponse).toList();
    }

    public InventoryResponse toInventoryResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        InventoryResponse dto = new InventoryResponse();
        dto.setId(inventory.getId());
        dto.setProductId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setProductName(product.getName());
        dto.setPlantId(inventory.getPlant().getId());
        dto.setPlantName(inventory.getPlant().getName());
        dto.setQuantity(inventory.getQuantity());
        dto.setUnit(product.getUnit());
        dto.setReorderLevel(product.getReorderLevel());
        dto.setStatus(StockStatus.of(inventory));
        dto.setLastUpdated(inventory.getLastUpdated());
        return dto;
    }

    public List<InventoryResponse> toInventoryResponses(List<Inventory> inventories) {
        return inventories.stream().map(this::toInventoryResponse).toList();
    }

    public StockTransactionResponse toTransactionResponse(StockTransaction transaction) {
        Product product = transaction.getProduct();
        StockTransactionResponse dto = new StockTransactionResponse();
        dto.setId(transaction.getId());
        dto.setProductId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setProductName(product.getName());
        dto.setPlantId(transaction.getPlant().getId());
        dto.setPlantName(transaction.getPlant().getName());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setQuantity(transaction.getQuantity());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setRemarks(transaction.getRemarks());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setPerformedBy(transaction.getPerformedBy());
        return dto;
    }

    public List<StockTransactionResponse> toTransactionResponses(List<StockTransaction> transactions) {
        return transactions.stream().map(this::toTransactionResponse).toList();
    }

    public UserResponse toUserResponse(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    public List<UserResponse> toUserResponses(List<User> users) {
        return users.stream().map(this::toUserResponse).toList();
    }
}
