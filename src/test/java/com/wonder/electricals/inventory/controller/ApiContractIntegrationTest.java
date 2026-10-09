package com.wonder.electricals.inventory.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.entity.User;
import com.wonder.electricals.inventory.security.AppUserPrincipal;
import com.wonder.electricals.inventory.support.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contract of the {@code /api} surface: malformed input must answer 400 rather than
 * 500, and the three roles must see exactly the operations they are meant to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiContractIntegrationTest {

    private static final String ADMIN = "admin";
    private static final String MANAGER = "manager";
    private static final String VIEWER = "viewer";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private com.wonder.electricals.inventory.repository.CategoryRepository categoryRepository;
    @Autowired
    private com.wonder.electricals.inventory.repository.PlantRepository plantRepository;
    @Autowired
    private com.wonder.electricals.inventory.repository.ProductRepository productRepository;
    @Autowired
    private com.wonder.electricals.inventory.repository.InventoryRepository inventoryRepository;

    private Long productId;
    private Long plantId;
    private Long categoryId;

    /**
     * The stock reads and the view/edit pages need a real record, otherwise the
     * repositories return nothing and the endpoints legitimately answer 404.
     */
    @org.junit.jupiter.api.BeforeEach
    void createStockFixture() {
        var category = categoryRepository.save(
                TestFixtures.category(null, "Contract Category " + UUID.randomUUID()));
        var plant = plantRepository.save(
                TestFixtures.plant(null, "Contract Plant " + UUID.randomUUID()));
        var product = productRepository.save(TestFixtures.product(null,
                "CONTRACT-" + UUID.randomUUID().toString().substring(0, 8),
                "Contract Product", category, "Meter", 10));
        inventoryRepository.save(TestFixtures.inventory(null, product, plant, 250));
        productId = product.getId();
        plantId = plant.getId();
        categoryId = category.getId();
    }

    /**
 * Signs in as one of the three application roles. The principal is a real
 * {@link AppUserPrincipal} so endpoints such as {@code /api/auth/me}, which inspect the
 * principal type, behave exactly as they do after a genuine login.
 */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor as(String role) {
        Role resolved = switch (role) {
            case ADMIN -> Role.ADMIN;
            case MANAGER -> Role.INVENTORY_MANAGER;
            case VIEWER -> Role.VIEWER;
            default -> throw new IllegalArgumentException("unknown role " + role);
        };
        User account = new User();
        account.setId(1000L);
        account.setName("Contract " + resolved.name());
        account.setEmail(role + "@contract.local");
        account.setPassword("{noop}irrelevant");
        account.setRole(resolved);
        account.setActive(true);
        return user(new AppUserPrincipal(account));
    }

    static Stream<String> protectedApiEndpoints() {
        return Stream.of(
                "/api/products", "/api/categories", "/api/plants", "/api/suppliers",
                "/api/inventory", "/api/inventory/low-stock", "/api/transactions",
                "/api/reports/inventory", "/api/reports/plant-wise", "/api/reports/low-stock",
                "/api/reports/stock-in", "/api/reports/stock-out", "/api/reports/transactions",
                "/api/stock/current", "/api/auth/me");
    }

    @ParameterizedTest(name = "{0} rejects an anonymous caller with 401")
    @MethodSource("protectedApiEndpoints")
    @DisplayName("every REST endpoint requires authentication")
    void anonymousAccessIsRejected(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a VIEWER may read data but may not write")
    void viewerIsReadOnly() throws Exception {
        mockMvc.perform(get("/api/products").with(as(VIEWER))).andExpect(status().isOk());
        mockMvc.perform(post("/api/categories").with(as(VIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/stock/in").with(as(VIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/products/1").with(as(VIEWER)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an INVENTORY_MANAGER may move stock but may not manage users")
    void managerCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/products").with(as(MANAGER))).andExpect(status().isOk());
        mockMvc.perform(post("/api/stock/in").with(as(MANAGER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users").with(as(MANAGER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users").with(as(MANAGER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an ADMIN may reach every REST endpoint")
    void adminHasFullAccess() throws Exception {
        // /api/stock/current is the one read that needs its product and plant keys.
        List<String> failures = new ArrayList<>();
        protectedApiEndpoints().forEach(endpoint -> {
            String url = endpoint.equals("/api/stock/current")
                    ? endpoint + "?productId=" + productId + "&plantId=" + plantId : endpoint;
            try {
                int status = mockMvc.perform(get(url).with(as(ADMIN))).andReturn().getResponse().getStatus();
                if (status != 200) {
                    failures.add(url + " answered " + status);
                }
            } catch (Exception ex) {
                failures.add(url + " threw " + ex);
            }
        });
        org.junit.jupiter.api.Assertions.assertEquals(List.of(), failures,
                "ADMIN should be able to read every endpoint");
    }

    @Test
    @DisplayName("a non-numeric path variable answers 400 with a readable message")
    void nonNumericPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/inventory/product/not-a-number").with(as(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("not a valid Long")));
    }

    @Test
    @DisplayName("a non-numeric id in a lookup path answers 400")
    void nonNumericLookupIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/abc").with(as(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/suppliers/1.5").with(as(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("an unknown numeric id answers 404 with the JSON error shape")
    void unknownIdIsNotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999").with(as(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/products/999999"));
    }

    @Test
    @DisplayName("an unparsable transaction type answers 400")
    void unparsableFilterAnswersBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions?type=NOT_A_TYPE").with(as(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("an unreadable JSON body answers 400")
    void malformedJsonAnswersBadRequest() throws Exception {
        mockMvc.perform(post("/api/stock/in").with(as(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("a blank REST payload is rejected by validation with field errors listed")
    void blankPayloadListsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/categories").with(as(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("a required query parameter is reported as 400, not 500")
    void missingRequiredQueryParameterIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stock/current").with(as(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("productId")));
    }

    @Test
    @DisplayName("a VIEWER is not even shown the add and edit forms")
    void viewerCannotOpenWriteForms() throws Exception {
        List<String> forms = List.of("/products/new", "/categories/new", "/plants/new",
                "/suppliers/new", "/products/1/edit", "/categories/1/edit");
        List<String> allowed = new ArrayList<>();
        for (String form : forms) {
            int status = mockMvc.perform(get(form).with(as(VIEWER)))
                    .andReturn().getResponse().getStatus();
            if (status != 403) {
                allowed.add(form + " answered " + status);
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(List.of(), allowed,
                "a read-only role must not be offered write forms");
    }

@Test
    @DisplayName("an ADMIN and a MANAGER can still open the add and edit forms")
    void writeRolesCanOpenForms() throws Exception {
        for (String role : List.of(ADMIN, MANAGER)) {
            mockMvc.perform(get("/products/new").with(as(role)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/categories/" + categoryId + "/edit").with(as(role)))
                    .andExpect(status().isOk());
        }
    }

@Test
    @DisplayName("a VIEWER can still open the read-only screens")
    void viewerCanStillRead() throws Exception {
        mockMvc.perform(get("/products/" + productId).with(as(VIEWER))).andExpect(status().isOk());
        mockMvc.perform(get("/products").with(as(VIEWER))).andExpect(status().isOk());
        mockMvc.perform(get("/transactions").with(as(VIEWER))).andExpect(status().isOk());
    }

@Test
    @DisplayName("an unknown URL is a 404, not an internal error")
    void unknownUrlIsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(as(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // A page request is handed to the container error page, which renders the
        // styled templates/error.html with a 404 (confirmed against the live server).
        mockMvc.perform(get("/no-such-page").with(as(ADMIN)))
                .andExpect(forwardedUrl("/error"));
    }

    /**
     * CSRF protection is switched off for {@code /api/**} on purpose (session-cookie API,
     * see SecurityConfig), but the Thymeleaf forms must still be protected.
     */
    @Test
    @DisplayName("an unsupported method is a 405 with an Allow header, not a 500")
    void unsupportedMethodIs405() throws Exception {
        mockMvc.perform(delete("/api/categories/" + categoryId).with(as(ADMIN)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")));
    }

    @Test
    @DisplayName("an unsupported content type is a 415, not a 500")
    void unsupportedContentTypeIs415() throws Exception {
        mockMvc.perform(post("/api/categories").with(as(ADMIN))
                        .contentType(MediaType.TEXT_PLAIN).content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("a page form write without a CSRF token is refused")
    void pageWriteWithoutCsrfIsRefused() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(as(MANAGER))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "No Csrf Category"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a REST write without a CSRF token still reaches validation")
    void restWriteWithoutCsrfReachesValidation() throws Exception {
        mockMvc.perform(post("/api/categories").with(as(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("the error response never leaks a stack trace or exception class name")
    void errorResponseHidesInternals() throws Exception {
        String body = mockMvc.perform(get("/api/products/abc").with(as(ADMIN)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(body.contains("Exception"),
                "response exposed an exception name: " + body);
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\tat "),
                "response exposed a stack trace: " + body);
    }

    @Test
    @DisplayName("a HTML page request with a malformed id redirects back with a flash error")
    void pageRequestWithMalformedIdRedirects() throws Exception {
        mockMvc.perform(get("/products/abc").with(as(ADMIN)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("the roles used by these tests are the roles the application defines")
    void rolesMatchApplication() {
        org.junit.jupiter.api.Assertions.assertEquals(3, Role.values().length);
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new Role[]{Role.ADMIN, Role.INVENTORY_MANAGER, Role.VIEWER}, Role.values());
    }
}