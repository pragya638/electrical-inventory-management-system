package com.wonder.electricals.inventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.repository.UserRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full stock workflow through the REST layer on a real (H2) database:
 * create master data, Stock IN, verify inventory, Stock OUT, verify inventory,
 * reject an over-issue, and confirm the audit trail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PlantRepository plantRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private UserRepository userRepository;

    private Long productId;
    private Long plantId;

    @BeforeEach
    void setUpMasterData() {
        var category = categoryRepository.save(
                TestFixtures.category(null, "Cables " + System.nanoTime()));
        var product = productRepository.save(TestFixtures.product(null,
                "WF-" + System.nanoTime(), "Workflow Cable", category, "Meter", 50));
        var plant = plantRepository.save(TestFixtures.plant(null, "Workflow Plant " + System.nanoTime()));
        supplierRepository.save(TestFixtures.supplier(null, "WF-SUP-" + System.nanoTime(), "Workflow Supplier"));
        productId = product.getId();
        plantId = plant.getId();
    }

    private String movementJson(int quantity) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "productId", productId,
                "plantId", plantId,
                "quantity", quantity,
                "referenceNumber", "WF-REF",
                "remarks", "integration test"));
    }

    @Test
    @DisplayName("Stock IN then Stock OUT updates the inventory and writes audit rows")
    void fullStockWorkflow() throws Exception {
        // ---- Stock IN 100 -----------------------------------------------------
        mockMvc.perform(post("/api/stock/in")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("IN"))
                .andExpect(jsonPath("$.quantity").value(100));

        // ---- Inventory now shows 100 -----------------------------------------
        mockMvc.perform(get("/api/inventory/product/{id}", productId)
                        .with(user("viewer@demo.local").roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(100))
                .andExpect(jsonPath("$[0].status").value("IN_STOCK"));

        // ---- Stock OUT 30 -----------------------------------------------------
        mockMvc.perform(post("/api/stock/out")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("OUT"));

        // ---- Inventory now shows 70 ------------------------------------------
        mockMvc.perform(get("/api/inventory/product/{id}", productId)
                        .with(user("viewer@demo.local").roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(70));

        // ---- Transaction history holds both movements -------------------------
        mockMvc.perform(get("/api/transactions")
                        .with(user("viewer@demo.local").roles("VIEWER"))
                        .param("productId", String.valueOf(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // ---- The database agrees with the API ---------------------------------
        assertThat(inventoryRepository.findByProductIdAndPlantId(productId, plantId))
                .get()
                .extracting(row -> row.getQuantity())
                .isEqualTo(70);
    }

    @Test
    @DisplayName("rule 3: Stock OUT larger than available is rejected and stock is unchanged")
    void rejectsStockOutBeyondAvailable() throws Exception {
        mockMvc.perform(post("/api/stock/in")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(20)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/stock/out")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(25)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Insufficient stock")));

        // Inventory and history are untouched by the rejected request.
        assertThat(inventoryRepository.findByProductIdAndPlantId(productId, plantId))
                .get()
                .extracting(row -> row.getQuantity())
                .isEqualTo(20);
    }

    @Test
    @DisplayName("rule 6: quantity exactly at the reorder level already counts as low stock")
    void lowStockAppearsAtReorderLevel() throws Exception {
        mockMvc.perform(post("/api/stock/in")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(50)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/low-stock")
                        .with(user("viewer@demo.local").roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(50))
                .andExpect(jsonPath("$[0].reorderLevel").value(50))
                .andExpect(jsonPath("$[0].status").value("LOW_STOCK"));
    }

    @Test
    @DisplayName("rule 10: a product and plant can only ever have one inventory row")
    void productAndPlantCombinationIsUnique() throws Exception {
        mockMvc.perform(post("/api/stock/in")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(10)))
                .andExpect(status().isCreated());

        // A second receipt of the same product at the same plant must add to the
        // existing row instead of inserting a duplicate one.
        mockMvc.perform(post("/api/stock/in")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(5)))
                .andExpect(status().isCreated());

        List<Inventory> rows = inventoryRepository.findAllByProductId(productId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getQuantity()).isEqualTo(15);

        // Both movements are still recorded, so the audit trail stays complete.
        mockMvc.perform(get("/api/transactions")
                        .with(user("viewer@demo.local").roles("VIEWER"))
                        .param("productId", String.valueOf(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("rule 6: draining the stock to zero reports OUT OF STOCK")
    void drainingToZeroReportsOutOfStock() throws Exception {
        mockMvc.perform(post("/api/stock/in")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(50)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/stock/out")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(50)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/low-stock")
                        .with(user("viewer@demo.local").roles("VIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(0))
                .andExpect(jsonPath("$[0].status").value("OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("rule 7: creating a product with a duplicate code returns 409")
    void duplicateProductCodeReturnsConflict() throws Exception {
        String code = "DUP-" + System.nanoTime();
        var category = categoryRepository.findAll().get(0);

        String body = objectMapper.writeValueAsString(Map.of(
                "productCode", code,
                "name", "First Product",
                "categoryId", category.getId(),
                "unit", "Meter",
                "reorderLevel", 10));

        mockMvc.perform(post("/api/products")
                        .with(user("admin@demo.local").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String duplicate = objectMapper.writeValueAsString(Map.of(
                "productCode", code,
                "name", "Second Product",
                "categoryId", category.getId(),
                "unit", "Meter",
                "reorderLevel", 10));

        mockMvc.perform(post("/api/products")
                        .with(user("admin@demo.local").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicate))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(code)));
    }

    @Test
    @DisplayName("unknown resources return 404 with a structured error body")
    void unknownResourceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999")
                        .with(user("admin@demo.local").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/products/999999"));
    }

    @Test
    @DisplayName("the users API is ADMIN only")
    void usersApiIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("manager@demo.local").roles("INVENTORY_MANAGER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .with(user("admin@demo.local").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("passwords are stored as BCrypt hashes, never as plain text")
    void passwordsAreHashed() throws Exception {
        // The seeder is disabled on the test profile, so create a user the way the app does.
        var user = TestFixtures.user(null, "hash-check@demo.local", Role.ADMIN);
        user.setPassword(passwordEncoder.encode("Admin@123"));
        userRepository.save(user);

        var stored = userRepository.findByEmailIgnoreCase("hash-check@demo.local").orElseThrow();
        assertThat(stored.getPassword()).matches("\\$2[abxy]\\$\\d{2}\\$.+");
        assertThat(stored.getPassword()).isNotEqualTo("Admin@123");
        assertThat(passwordEncoder.matches("Admin@123", stored.getPassword())).isTrue();
    }
}