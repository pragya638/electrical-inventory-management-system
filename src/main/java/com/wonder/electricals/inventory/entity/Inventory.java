package com.wonder.electricals.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Current stock of one product at one plant.
 *
 * <p>There is no public {@code setQuantity} on purpose: stock may only change through
 * the Stock IN and Stock OUT operations implemented in the service layer, which keeps
 * the inventory table and the transaction log consistent with each other.
 */
@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_plant", columnNames = {"product_id", "plant_id"})
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    void touch() {
        this.lastUpdated = LocalDateTime.now();
    }

    /** Applies a stock IN movement. */
    public void increaseBy(int amount) {
        this.quantity += amount;
    }

    /**
     * Applies a stock OUT movement. The caller is responsible for having already
     * verified that {@code amount} does not exceed the available quantity.
     */
    public void decreaseBy(int amount) {
        this.quantity -= amount;
    }

    /**
     * Seeds the quantity of a brand new inventory row. Deliberately not a general
     * {@code setQuantity}: the "stock only moves through Stock IN / OUT" rule stays
     * visible in the code, and updates must go through {@link #increaseBy(int)} or
     * {@link #decreaseBy(int)}.
     */
    public void initialiseQuantity(int startingQuantity) {
        this.quantity = startingQuantity;
    }

    /**
     * Low stock is defined as {@code quantity <= product.reorderLevel}
     * (business rule 6). Out of stock is the special case of zero.
     * {@link com.wonder.electricals.inventory.dto.StockStatus#of(Inventory)} is the
     * single place this rule is implemented for callers outside this class.
     */
    public int getReorderLevel() {
        return product.getReorderLevel();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
