package com.wonder.electricals.inventory.repository;

import com.wonder.electricals.inventory.entity.Plant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantRepository extends JpaRepository<Plant, Long> {

    long countByActiveTrue();

    List<Plant> findAllByOrderByNameAsc();

    List<Plant> findByActiveTrueOrderByNameAsc();
}
