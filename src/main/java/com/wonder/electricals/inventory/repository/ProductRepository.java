package com.wonder.electricals.inventory.repository;

import com.wonder.electricals.inventory.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCaseAndIdNot(String productCode, Long id);

    long countByActiveTrue();

    @Query("select p from Product p join fetch p.category where p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    @Query("select p from Product p join fetch p.category order by p.productCode")
    List<Product> findAllWithCategory();

    @Query("select p from Product p join fetch p.category where p.active = true order by p.name")
    List<Product> findAllActiveWithCategory();
}
