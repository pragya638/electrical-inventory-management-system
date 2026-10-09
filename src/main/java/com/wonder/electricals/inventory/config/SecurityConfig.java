package com.wonder.electricals.inventory.config;

import com.wonder.electricals.inventory.security.RestAccessDeniedHandler;
import com.wonder.electricals.inventory.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration for both the Thymeleaf pages and the REST API.
 *
 * <p>Route groups:
 * <ul>
 *   <li>{@code /users/**} is ADMIN only.</li>
 *   <li>Mutating master-data and stock routes require ADMIN or INVENTORY_MANAGER.</li>
 *   <li>Read routes are open to every authenticated role including VIEWER.</li>
 * </ul>
 * Page requests are answered with redirects; API requests with JSON status codes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String MANAGER = "INVENTORY_MANAGER";

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler) {
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        // The Thymeleaf forms submit Spring Security's CSRF token automatically.
                        // REST clients (Postman, scripts) cannot, so the token check is skipped for
                        // the /api tree. Safe here because the API authenticates with a session
                        // cookie rather than with ambient credentials such as basic auth.
                        .ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        // --- Public endpoints -------------------------------------------------
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()

                        // --- User management: ADMIN only --------------------------------------
                        .requestMatchers("/users/**", "/api/users/**").hasRole(ADMIN)

                        // --- Write operations: ADMIN or INVENTORY_MANAGER ----------------------
                        .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**",
                                "/api/plants/**", "/api/suppliers/**").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/categories/**",
                                "/api/plants/**", "/api/suppliers/**").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**", "/api/categories/**",
                                "/api/plants/**", "/api/suppliers/**").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**",
                                "/api/plants/**", "/api/suppliers/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/stock/**").hasAnyRole(ADMIN, MANAGER)

                        .requestMatchers(HttpMethod.POST, "/products/**", "/categories/**",
                                "/plants/**", "/suppliers/**", "/stock/**").hasAnyRole(ADMIN, MANAGER)
                        // The add and edit forms are part of writing: a VIEWER is not even
                        // shown them, which keeps the navigation honest about what is allowed.
                        .requestMatchers(HttpMethod.GET, "/products/new", "/products/*/edit",
                                "/categories/new", "/categories/*/edit",
                                "/plants/new", "/plants/*/edit",
                                "/suppliers/new", "/suppliers/*/edit").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers("/stock/**").hasAnyRole(ADMIN, MANAGER)

                        // --- Everything else needs an authenticated user -----------------------
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(handling -> handling
                        // Both branches must be registered: with a single mapping Spring applies
                        // that entry point to every route instead of matching on the path.
                        .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**"))
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
                                new AntPathRequestMatcher("/**"))
                        .defaultAccessDeniedHandlerFor(restAccessDeniedHandler,
                                new AntPathRequestMatcher("/api/**"))
                        .defaultAccessDeniedHandlerFor(new AccessDeniedHandlerImpl(),
                                new AntPathRequestMatcher("/**")));

        return http.build();
    }

    /** BCrypt with Spring Security's default strength (10). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the authentication manager as a bean so the REST login endpoint can
     * authenticate a JSON payload directly instead of going through the form filter.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }
}
