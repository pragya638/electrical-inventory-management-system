package com.wonder.electricals.inventory.support;

import com.wonder.electricals.inventory.dto.StockMovementRequest;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.entity.StockTransaction;
import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.entity.Supplier;
import com.wonder.electricals.inventory.entity.User;

/** Builders for test fixtures, so each test only states what it actually cares about. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(name + " description");
        category.setActive(true);
        return category;
    }

    public static Product product(Long id, String code, String name, Category category,
                                  String unit, int reorderLevel) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(code);
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setReorderLevel(reorderLevel);
        product.setActive(true);
        return product;
    }

    public static Plant plant(Long id, String name) {
        Plant plant = new Plant();
        plant.setId(id);
        plant.setName(name);
        plant.setLocation(name + " location");
        plant.setActive(true);
        return plant;
    }

    public static Supplier supplier(Long id, String code, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplierCode(code);
        supplier.setName(name);
        supplier.setActive(true);
        return supplier;
    }

    public static Inventory inventory(Long id, Product product, Plant plant, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setProduct(product);
        inventory.setPlant(plant);
        inventory.initialiseQuantity(quantity);
        inventory.setLastUpdated(java.time.LocalDateTime.now());
        return inventory;
    }

    public static StockTransaction transaction(Long id, Product product, Plant plant,
                                              TransactionType type, int quantity) {
        StockTransaction transaction = new StockTransaction();
        transaction.setId(id);
        transaction.setProduct(product);
        transaction.setPlant(plant);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setTransactionDate(java.time.LocalDate.now());
        transaction.setPerformedBy("tester@demo.local");
        return transaction;
    }

    public static User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName("Test " + role);
        user.setEmail(email);
        user.setPassword("{noop}secret");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    public static StockMovementRequest movement(Long productId, Long plantId, int quantity) {
        StockMovementRequest request = new StockMovementRequest();
        request.setProductId(productId);
        request.setPlantId(plantId);
        request.setQuantity(quantity);
        request.setReferenceNumber("REF-TEST");
        request.setRemarks("test movement");
        return request;
    }
}