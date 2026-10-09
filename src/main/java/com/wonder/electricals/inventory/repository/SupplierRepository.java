package com.wonder.electricals.inventory.repository;

import com.wonder.electricals.inventory.entity.Supplier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierCodeIgnoreCase(String supplierCode);

    boolean existsBySupplierCodeIgnoreCase(String supplierCode);

    boolean existsBySupplierCodeIgnoreCaseAndIdNot(String supplierCode, Long id);

    long countByActiveTrue();

    List<Supplier> findAllByOrderBySupplierCodeAsc();

    List<Supplier> findByActiveTrueOrderByNameAsc();
}
