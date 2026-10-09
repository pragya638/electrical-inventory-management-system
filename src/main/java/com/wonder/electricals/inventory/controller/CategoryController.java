package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.CategoryRequest;
import com.wonder.electricals.inventory.dto.response.CategoryResponse;
import com.wonder.electricals.inventory.service.CategoryService;
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

/**
 * Category screens. The controller only routes requests and shuttles the form model;
 * uniqueness and validation rules live in the service and the DTO constraints.
 */
@Controller
@RequestMapping("/categories")
public class CategoryController {

    private static final String REDIRECT_LIST = "redirect:/categories";

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("categories", categoryService.findAll(search));
        model.addAttribute("search", search);
        return "categories/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("categoryForm", new CategoryRequest());
        model.addAttribute("formAction", "/categories");
        model.addAttribute("formTitle", "Add Category");
        return "categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return renderFormWithErrors(model, "Add Category", "/categories");
        }
        categoryService.create(request);
        return REDIRECT_LIST + "?created";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        CategoryResponse category = categoryService.findById(id);
        model.addAttribute("categoryForm", toForm(category));
        model.addAttribute("formAction", "/categories/" + id);
        model.addAttribute("formTitle", "Edit Category");
        return "categories/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return renderFormWithErrors(model, "Edit Category", "/categories/" + id);
        }
        categoryService.update(id, request);
        return REDIRECT_LIST + "?updated";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        categoryService.changeStatus(id, active);
        return REDIRECT_LIST + "?status";
    }

    private String renderFormWithErrors(Model model, String title, String action) {
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        return "categories/form";
    }

    private CategoryRequest toForm(CategoryResponse category) {
        CategoryRequest form = new CategoryRequest();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setActive(category.isActive());
        return form;
    }
}
