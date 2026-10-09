package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.ProductRequest;
import com.wonder.electricals.inventory.dto.filter.ProductFilter;
import com.wonder.electricals.inventory.dto.response.CategoryResponse;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.ProductResponse;
import com.wonder.electricals.inventory.service.CategoryService;
import com.wonder.electricals.inventory.service.InventoryService;
import com.wonder.electricals.inventory.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Product list, create, edit, view and activate/deactivate screens. */
@Controller
@RequestMapping("/products")
public class ProductController {

    private static final String REDIRECT_LIST = "redirect:/products";
    private static final String UNITS = "Meter,Piece,Roll,Kilogram,Box,Set,Length";

    private final ProductService productService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;

    public ProductController(ProductService productService,
                             CategoryService categoryService,
                             InventoryService inventoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(ProductFilter filter, Model model) {
        model.addAttribute("products", productService.findAll(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.findAll(null));
        return "products/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, new ProductRequest(), "Add Product", "/products");
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("productForm") ProductRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Add Product", "/products");
            return "products/form";
        }
        productService.create(request);
        return REDIRECT_LIST + "?created";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        prepareForm(model, toForm(productService.findById(id)), "Edit Product", "/products/" + id);
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productForm") ProductRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Edit Product", "/products/" + id);
            return "products/form";
        }
        productService.update(id, request);
        return REDIRECT_LIST + "?updated";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        ProductResponse product = productService.findById(id);
        List<InventoryResponse> stock = inventoryService.findByProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("stockRows", stock);
        return "products/view";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        productService.changeStatus(id, active);
        return REDIRECT_LIST + "?status";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return REDIRECT_LIST + "?deleted";
    }

    private void prepareForm(Model model, ProductRequest form, String title, String action) {
        List<CategoryResponse> categories = categoryService.findAll(null);
        model.addAttribute("productForm", form);
        model.addAttribute("categories", categories);
        model.addAttribute("units", UNITS);
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
    }

    private ProductRequest toForm(ProductResponse product) {
        ProductRequest form = new ProductRequest();
        form.setProductCode(product.getProductCode());
        form.setName(product.getName());
        form.setCategoryId(product.getCategoryId());
        form.setUnit(product.getUnit());
        form.setReorderLevel(product.getReorderLevel());
        form.setDescription(product.getDescription());
        form.setActive(product.isActive());
        return form;
    }
}
