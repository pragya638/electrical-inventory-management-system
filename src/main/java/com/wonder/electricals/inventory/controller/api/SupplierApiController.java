package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.StatusUpdateRequest;
import com.wonder.electricals.inventory.dto.SupplierRequest;
import com.wonder.electricals.inventory.dto.response.SupplierResponse;
import com.wonder.electricals.inventory.service.SupplierService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierApiController {

    private final SupplierService supplierService;

    public SupplierApiController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return supplierService.findAll();
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id,
                                  @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public SupplierResponse changeStatus(@PathVariable Long id,
                                         @Valid @RequestBody StatusUpdateRequest request) {
        return supplierService.changeStatus(id, request.getActive());
    }
}
