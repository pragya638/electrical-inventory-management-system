package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.SupplierRequest;
import com.wonder.electricals.inventory.dto.response.SupplierResponse;
import com.wonder.electricals.inventory.entity.Supplier;
import com.wonder.electricals.inventory.exception.DuplicateResourceException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Supplier master data. Enforces the unique supplier code rule (business rule 8). */
@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final DtoMapper mapper;

    public SupplierService(SupplierRepository supplierRepository, DtoMapper mapper) {
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
    }

    public List<SupplierResponse> findAll() {
        return mapper.toSupplierResponses(supplierRepository.findAllByOrderBySupplierCodeAsc());
    }

    public List<SupplierResponse> findActive() {
        return mapper.toSupplierResponses(supplierRepository.findByActiveTrueOrderByNameAsc());
    }

    public SupplierResponse findById(Long id) {
        return mapper.toSupplierResponse(getEntity(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String code = request.getSupplierCode().trim();
        if (supplierRepository.existsBySupplierCodeIgnoreCase(code)) {
            throw DuplicateResourceException.of("supplier code", code);
        }
        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        supplier.setSupplierCode(code);
        return mapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = getEntity(id);
        String code = request.getSupplierCode().trim();
        if (supplierRepository.existsBySupplierCodeIgnoreCaseAndIdNot(code, id)) {
            throw DuplicateResourceException.of("supplier code", code);
        }
        supplier.setSupplierCode(code);
        applyRequest(supplier, request);
        return mapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse changeStatus(Long id, boolean active) {
        Supplier supplier = getEntity(id);
        supplier.setActive(active);
        return mapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName().trim());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }
    }

    private Supplier getEntity(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }
}
