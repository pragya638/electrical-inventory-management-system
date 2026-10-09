package com.wonder.electricals.inventory.controller.api;

import com.wonder.electricals.inventory.dto.LoginRequest;
import com.wonder.electricals.inventory.dto.response.LoginResponse;
import com.wonder.electricals.inventory.security.AppUserPrincipal;
import com.wonder.electricals.inventory.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session based login for API clients such as Postman. The endpoint reuses the same
 * {@link AuthenticationManager} as the HTML login form, so password hashing and the
 * authentication rules are identical for both.
 *
 * <p>Send the returned {@code JSESSIONID} cookie back on later calls.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;

    public AuthApiController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim(), request.getPassword()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        List<String> roles = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(new LoginResponse(principal.getId(), principal.getName(), principal.getEmail(),
                        principal.getRole().name(), roles, "Login successful"));
    }

    /** Confirms the session is still valid and returns the current principal. */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> currentUser() {
        return SecurityUtils.currentUser()
                .map(user -> ResponseEntity.ok(new LoginResponse(
                        user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                        user.getAuthorities().stream()
                                .map(authority -> authority.getAuthority()).toList(),
                        "Authenticated session")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
