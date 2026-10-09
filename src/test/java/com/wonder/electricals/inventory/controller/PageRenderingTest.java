package com.wonder.electricals.inventory.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.entity.Supplier;
import com.wonder.electricals.inventory.entity.User;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.repository.UserRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end checks against the full Spring context on the H2 test profile.
 * Verifies that every Thymeleaf page renders for each role and that the protected
 * routes return the correct status codes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PageRenderingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PlantRepository plantRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("unauthenticated users are redirected to the login page")
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("the login page is publicly accessible")
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));
    }

    @Test
    @DisplayName("the dashboard renders for an admin")
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void dashboardRendersForAdmin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")));
    }

    @Test
    @DisplayName("every management page renders for an inventory manager")
    @WithMockUser(username = "manager@demo.local", roles = "INVENTORY_MANAGER")
    void managementPagesRenderForManager() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/products/new")).andExpect(status().isOk());
        mockMvc.perform(get("/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/categories/new")).andExpect(status().isOk());
        mockMvc.perform(get("/plants")).andExpect(status().isOk());
        mockMvc.perform(get("/plants/new")).andExpect(status().isOk());
        mockMvc.perform(get("/suppliers")).andExpect(status().isOk());
        mockMvc.perform(get("/suppliers/new")).andExpect(status().isOk());
        mockMvc.perform(get("/inventory")).andExpect(status().isOk());
        mockMvc.perform(get("/stock/in")).andExpect(status().isOk());
        mockMvc.perform(get("/stock/out")).andExpect(status().isOk());
        mockMvc.perform(get("/transactions")).andExpect(status().isOk());
        mockMvc.perform(get("/low-stock")).andExpect(status().isOk());
        mockMvc.perform(get("/reports")).andExpect(status().isOk());
        mockMvc.perform(get("/reports/inventory")).andExpect(status().isOk());
        mockMvc.perform(get("/reports/low-stock")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("every read-only page renders for a viewer")
    @WithMockUser(username = "viewer@demo.local", roles = "VIEWER")
    void readOnlyPagesRenderForViewer() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/inventory")).andExpect(status().isOk());
        mockMvc.perform(get("/transactions")).andExpect(status().isOk());
        mockMvc.perform(get("/low-stock")).andExpect(status().isOk());
        mockMvc.perform(get("/reports")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a viewer cannot reach the stock IN page")
    @WithMockUser(username = "viewer@demo.local", roles = "VIEWER")
    void viewerCannotOpenStockInPage() throws Exception {
        mockMvc.perform(get("/stock/in"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a viewer cannot reach the users page")
    @WithMockUser(username = "viewer@demo.local", roles = "VIEWER")
    void viewerCannotOpenUsersPage() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a viewer cannot POST a product (blocked by the URL rules)")
    @WithMockUser(username = "viewer@demo.local", roles = "VIEWER")
    void viewerCannotCreateProduct() throws Exception {
        String body = objectMapper.writeValueAsString(new ProductPayload(
                "CBL-999", "Blocked Product", 1L, "Meter", 10));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an inventory manager cannot reach the users page")
    @WithMockUser(username = "manager@demo.local", roles = "INVENTORY_MANAGER")
    void managerCannotOpenUsersPage() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("stock OUT POST by a viewer is rejected")
    @WithMockUser(username = "viewer@demo.local", roles = "VIEWER")
    void viewerCannotPostStockOut() throws Exception {
        String body = objectMapper.writeValueAsString(new StockPayload(1L, 1L, 5));

        mockMvc.perform(post("/api/stock/out")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

@Test
    @DisplayName("an admin can reach the users page")
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void adminCanOpenUsersPage() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isOk());
    }

    /**
     * Regression guard for the HTML forms. A Thymeleaf binding mistake (for example
     * a checkbox bound to a non-boolean property) only shows up when the template is
     * actually rendered, so every add and edit form is requested here.
     */
    @Test
    @DisplayName("every add and edit form renders for an admin")
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void adminFormsRender() throws Exception {
        long suffix = System.nanoTime();
        Category category = categoryRepository.save(TestFixtures.category(null, "Form Check " + suffix));
        Product product = productRepository.save(TestFixtures.product(null, "FRM-" + suffix,
                "Form Check Product", category, "Meter", 10));
        Plant plant = plantRepository.save(TestFixtures.plant(null, "Form Check Plant " + suffix));
        Supplier supplier = supplierRepository.save(
                TestFixtures.supplier(null, "FRM-SUP-" + suffix, "Form Check Supplier"));
        User user = userRepository.save(TestFixtures.user(null, "form-check-" + suffix + "@demo.local", Role.VIEWER));

        mockMvc.perform(get("/products/new")).andExpect(status().isOk());
        mockMvc.perform(get("/products/{id}/edit", product.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/categories/new")).andExpect(status().isOk());
        mockMvc.perform(get("/categories/{id}/edit", category.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/plants/new")).andExpect(status().isOk());
        mockMvc.perform(get("/plants/{id}/edit", plant.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/plants/{id}", plant.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/suppliers/new")).andExpect(status().isOk());
        mockMvc.perform(get("/suppliers/{id}/edit", supplier.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/suppliers/{id}", supplier.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/users/new")).andExpect(status().isOk());
        mockMvc.perform(get("/users/{id}/edit", user.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/products/{id}", product.getId())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a missing page returns the user to where they came from")
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void missingProductRedirectsBackToReferrer() throws Exception {
        mockMvc.perform(get("/products/999999")
                        .header("Referer", "http://localhost:8080/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:8080/products"));
    }

    @Test
    @DisplayName("a missing page with no referrer falls back to the dashboard")
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void missingProductWithoutReferrerFallsBackToDashboard() throws Exception {
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("a blank categoryId is rejected with 400 and a structured error")
    @WithMockUser(username = "admin@demo.local", roles = "ADMIN")
    void invalidProductPayloadReturnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(new ProductPayload(
                "CBL-999", "Blocked Product", null, "Meter", 10));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    /** JSON body for product creation. */
    record ProductPayload(String productCode, String name, Long categoryId, String unit,
                          Integer reorderLevel) {
    }

    /** JSON body for a stock movement. */
    record StockPayload(Long productId, Long plantId, Integer quantity) {
    }
}