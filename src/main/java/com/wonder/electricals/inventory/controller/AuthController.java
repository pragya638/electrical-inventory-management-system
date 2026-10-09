package com.wonder.electricals.inventory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Renders the login screen. Form submission is handled by Spring Security. */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Convenience redirect so {@code /} always lands somewhere useful. */
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
