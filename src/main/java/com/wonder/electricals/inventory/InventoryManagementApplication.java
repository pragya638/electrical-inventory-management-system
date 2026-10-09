package com.wonder.electricals.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point of the Electrical Inventory andamp; Stock Management System.
 *
 * <p>This is a layered monolithic Spring Boot prototype built for internship-level learning.
 * All organisation, plant, supplier and product names shipped with the application are
 * demonstration data and do not represent real operational records.
 */
@SpringBootApplication
@EnableJpaAuditing
public class InventoryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryManagementApplication.class, args);
    }
}
