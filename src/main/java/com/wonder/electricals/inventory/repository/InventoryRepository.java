package com.wonder.electricals.inventory.repository;

import com.wonder.electricals.inventory.entity.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndPlantId(Long productId, Long plantId);

    /**
     * Low stock is {@code quantity <= product.reorderLevel} (business rule 6).
     * Expressed as a JPQL query so the comparison happens in the database instead
     * of pulling the whole inventory table into memory.
     */
    @Query("""
            select i from Inventory i
            join fetch i.product p
            join fetch i.plant
            where i.quantity <= p.reorderLevel
            order by i.quantity asc, p.name asc
            """)
    List<Inventory> findLowStock();

    @Query("select coalesce(sum(i.quantity), 0) from Inventory i")
    long totalQuantity();

    @Query("select coalesce(sum(i.quantity), 0) from Inventory i where i.plant.id = :plantId")
    long totalQuantityByPlant(@Param("plantId") Long plantId);

    @Query("""
            select i from Inventory i
            join fetch i.product p
            join fetch i.plant
            where (:productId is null or i.product.id = :productId)
              and (:plantId is null or i.plant.id = :plantId)
              and (:status is null or
                   (:status = 'IN_STOCK' and i.quantity > p.reorderLevel) or
                   (:status = 'LOW_STOCK' and i.quantity > 0 and i.quantity <= p.reorderLevel) or
                   (:status = 'OUT_OF_STOCK' and i.quantity = 0))
            order by p.name asc, i.plant.name asc
            """)
    List<Inventory> search(@Param("productId") Long productId,
                           @Param("plantId") Long plantId,
                           @Param("status") String status);

    @Query("""
            select i from Inventory i
            join fetch i.product p
            join fetch i.plant
            where i.product.id = :productId
            order by i.plant.name asc
            """)
    List<Inventory> findByProductId(@Param("productId") Long productId);

    @Query("""
            select i from Inventory i
            join fetch i.product p
            join fetch i.plant
            where i.plant.id = :plantId
            order by p.name asc
            """)
    List<Inventory> findByPlantId(@Param("plantId") Long plantId);

    @Query("""
            select i from Inventory i
            join fetch i.product p
            join fetch i.plant
            order by p.name asc, i.plant.name asc
            """)
    List<Inventory> findAllDetailed();

    @Query("select i from Inventory i join fetch i.product p where p.id = :productId")
    List<Inventory> findAllByProductId(@Param("productId") Long productId);
}
