package com.wonder.electricals.inventory.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Small helpers so services do not repeat the null checks around the principal. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AppUserPrincipal> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /** Email of the signed-in user, used as {@code performedBy} on a transaction. */
    public static String currentUsername() {
        return currentUser().map(AppUserPrincipal::getEmail).orElse("system");
    }
}
