package com.wonder.electricals.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wonder.electricals.inventory.dto.UserRequest;
import com.wonder.electricals.inventory.dto.response.UserResponse;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.entity.User;
import com.wonder.electricals.inventory.exception.DuplicateResourceException;
import com.wonder.electricals.inventory.exception.OperationNotAllowedException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.UserRepository;
import com.wonder.electricals.inventory.support.TestFixtures;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** User management rules, including password hashing. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder, new DtoMapper());
    }

    private UserRequest request(String email, Role role, String password) {
        UserRequest request = new UserRequest();
        request.setName("Demo User");
        request.setEmail(email);
        request.setRole(role);
        request.setPassword(password);
        return request;
    }

    @Test
    @DisplayName("stores a BCrypt hash, never the plain password")
    void storesBcryptHash() {
        when(userRepository.existsByEmailIgnoreCase("new@demo.local")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = userService.create(
                request("new@demo.local", Role.VIEWER, "Secret@123"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("new@demo.local");

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String stored = captor.getValue().getPassword();

        assertThat(stored).isNotEqualTo("Secret@123");
        assertThat(stored).matches("\\$2[abxy]\\$\\d{2}\\$.+");
        assertThat(passwordEncoder.matches("Secret@123", stored)).isTrue();
    }

    @Test
    @DisplayName("rejects a duplicate email")
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@demo.local")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.create(request("dup@demo.local", Role.VIEWER, "Secret@123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("requires a password on create")
    void requiresPasswordOnCreate() {
        when(userRepository.existsByEmailIgnoreCase("new@demo.local")).thenReturn(false);

        assertThatThrownBy(() -> userService.create(request("new@demo.local", Role.VIEWER, "  ")))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("password is required");
    }

    @Test
    @DisplayName("keeps the existing password when the update leaves it blank")
    void keepsPasswordWhenBlank() {
        User existing = TestFixtures.user(4L, "keep@demo.local", Role.VIEWER);
        String originalHash = passwordEncoder.encode("Original@123");
        existing.setPassword(originalHash);

        when(userRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("keep@demo.local")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserRequest update = request("keep@demo.local", Role.INVENTORY_MANAGER, "");
        userService.update(4L, update);

        assertThat(existing.getPassword()).isEqualTo(originalHash);
        assertThat(existing.getRole()).isEqualTo(Role.INVENTORY_MANAGER);
    }

    @Test
    @DisplayName("refuses to deactivate the last active admin")
    void refusesToDeactivateLastAdmin() {
        User onlyAdmin = TestFixtures.user(1L, "admin@demo.local", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.findAll()).thenReturn(List.of(onlyAdmin));

        assertThatThrownBy(() -> userService.changeStatus(1L, false))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("active ADMIN");
    }

    @Test
    @DisplayName("reports a missing user as not found")
    void reportsMissingUser() {
        when(userRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(88L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}