package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.UserRequest;
import com.wonder.electricals.inventory.dto.response.UserResponse;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.service.UserService;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** User administration screens. Reachable by ADMIN only, enforced in SecurityConfig. */
@Controller
@RequestMapping("/users")
public class UserController {

    private static final String REDIRECT_LIST = "redirect:/users";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        prepareForm(model, new UserRequest(), "Add User", "/users");
        return "users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") UserRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Add User", "/users");
            return "users/form";
        }
        userService.create(request);
        return REDIRECT_LIST + "?created";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        prepareForm(model, toForm(userService.findById(id)), "Edit User", "/users/" + id);
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") UserRequest request,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, request, "Edit User", "/users/" + id);
            return "users/form";
        }
        userService.update(id, request);
        return REDIRECT_LIST + "?updated";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        userService.changeStatus(id, active);
        return REDIRECT_LIST + "?status";
    }

    private void prepareForm(Model model, UserRequest form, String title, String action) {
        model.addAttribute("userForm", form);
        model.addAttribute("roles", Arrays.asList(Role.values()));
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
    }

    private UserRequest toForm(UserResponse user) {
        UserRequest form = new UserRequest();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setActive(user.isActive());
        return form;
    }
}
