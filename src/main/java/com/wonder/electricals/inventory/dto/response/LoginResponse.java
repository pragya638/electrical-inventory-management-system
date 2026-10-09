package com.wonder.electricals.inventory.dto.response;

import java.util.List;

public class LoginResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String message;
    private List<String> roles;

    public LoginResponse(Long id, String name, String email, String role, List<String> roles,
                         String message) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.roles = roles;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getRoles() {
        return roles;
    }
}
