package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.UserRequest;
import com.wonder.electricals.inventory.dto.response.UserResponse;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.entity.User;
import com.wonder.electricals.inventory.exception.DuplicateResourceException;
import com.wonder.electricals.inventory.exception.OperationNotAllowedException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.UserRepository;
import com.wonder.electricals.inventory.security.SecurityUtils;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User administration. Passwords are always stored as BCrypt hashes. */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper mapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, DtoMapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    public List<UserResponse> findAll() {
        return mapper.toUserResponses(userRepository.findAll());
    }

    public UserResponse findById(Long id) {
        return mapper.toUserResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = normaliseEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateResourceException.of("email", email);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new OperationNotAllowedException("A password is required when creating a user");
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
user.setRole(request.getRole());
        user.setActive(request.getActive() == null || request.getActive());
        return mapper.toUserResponse(userRepository.save(user));
    }

    /** Updating a user never changes the password unless a new one is supplied. */
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = getEntity(id);
        String email = normaliseEmail(request.getEmail());
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw DuplicateResourceException.of("email", email);
                });
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return mapper.toUserResponse(userRepository.save(user));
    }

    /**
     * Deactivates rather than deletes, and stops the last enabled admin from locking
     * everyone out of user management.
     */
    @Transactional
    public UserResponse changeStatus(Long id, boolean active) {
        User user = getEntity(id);
        if (!active && user.getRole() == Role.ADMIN
                && countActiveAdmins() <= 1) {
            throw new OperationNotAllowedException(
                    "At least one active ADMIN account must remain");
        }
        if (!active && SecurityUtils.currentUser()
                .map(current -> current.getId().equals(id))
                .orElse(false)) {
            throw new OperationNotAllowedException("You cannot deactivate your own account");
        }
        user.setActive(active);
        return mapper.toUserResponse(userRepository.save(user));
    }

    private long countActiveAdmins() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();
    }

    private String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
