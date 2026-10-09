package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.PlantRequest;
import com.wonder.electricals.inventory.dto.StatusUpdateRequest;
import com.wonder.electricals.inventory.dto.response.PlantResponse;
import com.wonder.electricals.inventory.service.PlantService;
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
@RequestMapping("/api/plants")
public class PlantApiController {

    private final PlantService plantService;

    public PlantApiController(PlantService plantService) {
        this.plantService = plantService;
    }

    @PostMapping
    public ResponseEntity<PlantResponse> create(@Valid @RequestBody PlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantService.create(request));
    }

    @GetMapping
    public List<PlantResponse> list() {
        return plantService.findAll();
    }

    @GetMapping("/{id}")
    public PlantResponse get(@PathVariable Long id) {
        return plantService.findById(id);
    }

    @PutMapping("/{id}")
    public PlantResponse update(@PathVariable Long id, @Valid @RequestBody PlantRequest request) {
        return plantService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public PlantResponse changeStatus(@PathVariable Long id,
                                      @Valid @RequestBody StatusUpdateRequest request) {
        return plantService.changeStatus(id, request.getActive());
    }
}
