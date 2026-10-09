package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.PlantRequest;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.PlantResponse;
import com.wonder.electricals.inventory.service.InventoryService;
import com.wonder.electricals.inventory.service.PlantService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Plant list, create, edit, view and activate/deactivate screens. */
@Controller
@RequestMapping("/plants")
public class PlantController {

    private static final String REDIRECT_LIST = "redirect:/plants";

    private final PlantService plantService;
    private final InventoryService inventoryService;

    public PlantController(PlantService plantService, InventoryService inventoryService) {
        this.plantService = plantService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("plants", plantService.findAll());
        return "plants/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, new PlantRequest(), "Add Plant", "/plants");
        return "plants/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("plantForm") PlantRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Add Plant", "/plants");
            return "plants/form";
        }
        plantService.create(request);
        return REDIRECT_LIST + "?created";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        prepareForm(model, toForm(plantService.findById(id)), "Edit Plant", "/plants/" + id);
        return "plants/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("plantForm") PlantRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Edit Plant", "/plants/" + id);
            return "plants/form";
        }
        plantService.update(id, request);
        return REDIRECT_LIST + "?updated";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        PlantResponse plant = plantService.findById(id);
        model.addAttribute("plant", plant);
        model.addAttribute("stockRows", inventoryService.findByPlant(id));
        return "plants/view";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        plantService.changeStatus(id, active);
        return REDIRECT_LIST + "?status";
    }

    private void prepareForm(Model model, PlantRequest form, String title, String action) {
        model.addAttribute("plantForm", form);
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
    }

    private PlantRequest toForm(PlantResponse plant) {
        PlantRequest form = new PlantRequest();
        form.setName(plant.getName());
        form.setLocation(plant.getLocation());
        form.setContactPerson(plant.getContactPerson());
        form.setContactNumber(plant.getContactNumber());
        form.setActive(plant.isActive());
        return form;
    }
}
