package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.PlantRequest;
import com.wonder.electricals.inventory.dto.response.PlantResponse;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Plant (stocking location) master data. */
@Service
@Transactional(readOnly = true)
public class PlantService {

    private final PlantRepository plantRepository;
    private final DtoMapper mapper;

    public PlantService(PlantRepository plantRepository, DtoMapper mapper) {
        this.plantRepository = plantRepository;
        this.mapper = mapper;
    }

    public List<PlantResponse> findAll() {
        return mapper.toPlantResponses(plantRepository.findAllByOrderByNameAsc());
    }

    public List<PlantResponse> findActive() {
        return mapper.toPlantResponses(plantRepository.findByActiveTrueOrderByNameAsc());
    }

    public PlantResponse findById(Long id) {
        return mapper.toPlantResponse(getEntity(id));
    }

    @Transactional
    public PlantResponse create(PlantRequest request) {
        Plant plant = new Plant();
        applyRequest(plant, request);
        return mapper.toPlantResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantResponse update(Long id, PlantRequest request) {
        Plant plant = getEntity(id);
        applyRequest(plant, request);
        return mapper.toPlantResponse(plantRepository.save(plant));
    }

    @Transactional
    public PlantResponse changeStatus(Long id, boolean active) {
        Plant plant = getEntity(id);
        plant.setActive(active);
        return mapper.toPlantResponse(plantRepository.save(plant));
    }

    private void applyRequest(Plant plant, PlantRequest request) {
        plant.setName(request.getName().trim());
        plant.setLocation(request.getLocation());
        plant.setContactPerson(request.getContactPerson());
        plant.setContactNumber(request.getContactNumber());
        if (request.getActive() != null) {
            plant.setActive(request.getActive());
        }
    }

    private Plant getEntity(Long id) {
        return plantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Plant", id));
    }
}
