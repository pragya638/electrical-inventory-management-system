package com.wonder.electricals.inventory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.wonder.electricals.inventory.entity.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verifies the Jakarta Bean Validation constraints on the request DTOs. */
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private Set<String> messagesFor(Object object) {
        return validator.validate(object).stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("ProductRequest rejects a blank code, blank name and negative reorder level")
    void productRequestRejectsInvalidInput() {
        ProductRequest request = new ProductRequest();
        request.setProductCode("  ");
        request.setName("");
        request.setReorderLevel(-5);

        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("productCode"));
        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("name"));
        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("reorderLevel"));
    }

    @Test
    @DisplayName("ProductRequest accepts a valid payload")
    void productRequestAcceptsValidInput() {
        ProductRequest request = new ProductRequest();
        request.setProductCode("CBL-001");
        request.setName("Copper Cable");
        request.setCategoryId(1L);
        request.setUnit("Meter");
        request.setReorderLevel(0);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("StockMovementRequest requires a positive quantity")
    void stockMovementRequiresPositiveQuantity() {
        StockMovementRequest request = new StockMovementRequest();
        request.setProductId(1L);
        request.setPlantId(1L);
        request.setQuantity(0);

        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("quantity")
                && m.contains("greater than zero"));
    }

    @Test
    @DisplayName("StockMovementRequest rejects an unknown productId")
    void stockMovementRejectsInvalidIds() {
        StockMovementRequest request = new StockMovementRequest();
        request.setProductId(-1L);
        request.setPlantId(0L);
        request.setQuantity(5);

        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("productId"));
        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("plantId"));
    }

    @Test
    @DisplayName("UserRequest validates the email format")
    void userRequestValidatesEmail() {
        UserRequest request = new UserRequest();
        request.setName("Demo");
        request.setEmail("not-an-email");
        request.setRole(Role.VIEWER);
        request.setPassword("short");

        Set<String> messages = messagesFor(request);
        assertThat(messages).anyMatch(m -> m.startsWith("email"));
        assertThat(messages).anyMatch(m -> m.startsWith("password"));
    }

    @Test
    @DisplayName("UserRequest accepts a valid payload")
    void userRequestAcceptsValidInput() {
        UserRequest request = new UserRequest();
        request.setName("Demo Admin");
        request.setEmail("admin@demo.local");
        request.setRole(Role.ADMIN);
        request.setPassword("Admin@123");
        request.setActive(true);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("LoginRequest requires both email and password")
    void loginRequestRequiresCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword(null);

        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("email"));
        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("password"));
    }

    @Test
    @DisplayName("SupplierRequest validates the email and phone formats")
    void supplierRequestValidatesFormats() {
        SupplierRequest request = new SupplierRequest();
        request.setSupplierCode("SUP-001");
        request.setName("Demo");
        request.setEmail("bad-email");
        request.setPhone("abc");

        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("email"));
        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("phone"));
    }

    @Test
    @DisplayName("PlantRequest validates the contact number format")
    void plantRequestValidatesContactNumber() {
        PlantRequest request = new PlantRequest();
        request.setName("Plant X");
        request.setContactNumber("12");

        assertThat(messagesFor(request)).anyMatch(m -> m.startsWith("contactNumber"));
    }

    @Test
    @DisplayName("StatusUpdateRequest requires the active flag")
    void statusUpdateRequiresFlag() {
        StatusUpdateRequest request = new StatusUpdateRequest();

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("active"));
    }
}