package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.StockMovementRequest;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.service.PlantService;
import com.wonder.electricals.inventory.service.ProductService;
import com.wonder.electricals.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Stock IN and Stock OUT screens. Both share one form template; the movement is
 * decided by which URL was posted to, never by a hidden field.
 */
@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;
    private final ProductService productService;
    private final PlantService plantService;

    public StockController(StockService stockService,
                           ProductService productService,
                           PlantService plantService) {
        this.stockService = stockService;
        this.productService = productService;
        this.plantService = plantService;
    }

    @GetMapping("/in")
    public String showStockInForm(Model model) {
        prepareForm(model, new StockMovementRequest(), "Stock IN", "/stock/in");
        return "stock/form";
    }

    @PostMapping("/in")
    public String stockIn(@Valid @ModelAttribute("stockForm") StockMovementRequest request,
                          BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Stock IN", "/stock/in");
            return "stock/form";
        }
        StockTransactionResponse transaction = stockService.stockIn(request);
        return "redirect:/stock/in?success=" + describe(transaction, "received");
    }

    @GetMapping("/out")
    public String showStockOutForm(@RequestParam(required = false) Long productId,
                                   @RequestParam(required = false) Long plantId,
                                   Model model) {
        StockMovementRequest form = new StockMovementRequest();
        form.setProductId(productId);
        form.setPlantId(plantId);
        prepareForm(model, form, "Stock OUT", "/stock/out");
        return "stock/form";
    }

    @PostMapping("/out")
    public String stockOut(@Valid @ModelAttribute("stockForm") StockMovementRequest request,
                           BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Stock OUT", "/stock/out");
            return "stock/form";
        }
        StockTransactionResponse transaction = stockService.stockOut(request);
        return "redirect:/stock/out?success=" + describe(transaction, "issued");
    }

    private void prepareForm(Model model, StockMovementRequest form, String title, String action) {
        model.addAttribute("stockForm", form);
        model.addAttribute("products", productService.findActive());
        model.addAttribute("plants", plantService.findActive());
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("isStockIn", "Stock IN".equals(title));
    }

    private String describe(StockTransactionResponse transaction, String verb) {
        return "Stock " + verb + ": " + transaction.getQuantity() + " x "
                + transaction.getProductCode() + " at " + transaction.getPlantName();
    }
}
