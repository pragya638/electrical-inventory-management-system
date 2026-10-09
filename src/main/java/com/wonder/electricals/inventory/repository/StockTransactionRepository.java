package com.wonder.electricals.inventory.repository;

import com.wonder.electricals.inventory.entity.StockTransaction;
import com.wonder.electricals.inventory.entity.TransactionType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    @Query("""
            select t from StockTransaction t
            join fetch t.product
            join fetch t.plant
            where (:type is null or t.transactionType = :type)
              and (:productId is null or t.product.id = :productId)
              and (:plantId is null or t.plant.id = :plantId)
              and (:from is null or t.transactionDate >= :from)
              and (:to is null or t.transactionDate <= :to)
            """)
    List<StockTransaction> search(@Param("type") TransactionType type,
                                  @Param("productId") Long productId,
                                  @Param("plantId") Long plantId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  Pageable pageable);

    @Query("""
            select t from StockTransaction t
            join fetch t.product
            join fetch t.plant
            where (:type is null or t.transactionType = :type)
              and (:from is null or t.transactionDate >= :from)
              and (:to is null or t.transactionDate <= :to)
            order by t.transactionDate desc, t.id desc
            """)
    List<StockTransaction> findForReport(@Param("type") TransactionType type,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    @Query("""
            select t from StockTransaction t join fetch t.product join fetch t.plant
            where t.transactionType = :type
            order by t.transactionDate desc, t.id desc
            """)
    List<StockTransaction> findRecentByType(@Param("type") TransactionType type, Pageable pageable);

    boolean existsByProductId(Long productId);
}
