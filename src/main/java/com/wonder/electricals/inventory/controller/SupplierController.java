package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.SupplierRequest;
import com.wonder.electricals.inventory.dto.response.SupplierResponse;
import com.wonder.electricals.inventory.service.SupplierService;
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

/** Supplier list, create, edit, view and activate/deactivate screens. */
@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private static final String REDIRECT_LIST = "redirect:/suppliers";

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "suppliers/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, new SupplierRequest(), "Add Supplier", "/suppliers");
        return "suppliers/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("supplierForm") SupplierRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Add Supplier", "/suppliers");
            return "suppliers/form";
        }
        supplierService.create(request);
        return REDIRECT_LIST + "?created";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        prepareForm(model, toForm(supplierService.findById(id)), "Edit Supplier", "/suppliers/" + id);
        return "suppliers/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("supplierForm") SupplierRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Edit Supplier", "/suppliers/" + id);
            return "suppliers/form";
        }
        supplierService.update(id, request);
        return REDIRECT_LIST + "?updated";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", supplierService.findById(id));
        return "suppliers/view";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        supplierService.changeStatus(id, active);
        return REDIRECT_LIST + "?status";
    }

    private void prepareForm(Model model, SupplierRequest form, String title, String action) {
        model.addAttribute("supplierForm", form);
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
    }

    private SupplierRequest toForm(SupplierResponse supplier) {
        SupplierRequest form = new SupplierRequest();
        form.setSupplierCode(supplier.getSupplierCode());
        form.setName(supplier.getName());
        form.setContactPerson(supplier.getContactPerson());
        form.setEmail(supplier.getEmail());
        form.setPhone(supplier.getPhone());
        form.setAddress(supplier.getAddress());
        form.setActive(supplier.isActive());
        return form;
    }
}
