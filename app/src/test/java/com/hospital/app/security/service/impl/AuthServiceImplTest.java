package com.hospital.app.security.service.impl;

import com.hospital.app.security.dto.request.RegisterRequest;
import com.hospital.app.security.dto.response.AuthResponse;
import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.RoleType;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.exception.EmailAlreadyExistsException;
import com.hospital.app.security.exception.RoleNotFoundException;
import com.hospital.app.security.repository.RoleRepository;
import com.hospital.app.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role doctorRole;
    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        doctorRole = Role.builder()
                .roleId(UUID.randomUUID())
                .name(RoleType.DOCTOR)
                .build();

        validRequest = new RegisterRequest(
                "Dr. John Doe",
                "john.doe@clinic.com",
                "securePass123",
                RoleType.DOCTOR
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register() — Happy Path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() — should save user and return AuthResponse on happy path")
    void register_shouldSaveUserAndReturnResponse() {
        // Arrange
        UUID generatedId = UUID.randomUUID();
        User savedUser = User.builder()
                .userId(generatedId)
                .name("Dr. John Doe")
                .email("john.doe@clinic.com")
                .passwordHash("$2a$10$hashed")
                .isEnabled(true)
                .roles(Set.of(doctorRole))
                .build();

        when(userRepository.existsByEmail(validRequest.email())).thenReturn(false);
        when(roleRepository.findByName(RoleType.DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(passwordEncoder.encode(validRequest.password())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        AuthResponse response = authService.register(validRequest);

        // Assert
        assertThat(response.userId()).isEqualTo(generatedId);
        assertThat(response.email()).isEqualTo("john.doe@clinic.com");
        assertThat(response.name()).isEqualTo("Dr. John Doe");
        assertThat(response.role()).isEqualTo("DOCTOR");
        assertThat(response.message()).isEqualTo("User registered successfully");
    }

    @Test
    @DisplayName("register() — should encode password with BCrypt before saving")
    void register_shouldEncodePasswordBeforeSave() {
        // Arrange
        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .name(validRequest.name())
                .email(validRequest.email())
                .passwordHash("$2a$10$encodedHash")
                .isEnabled(true)
                .roles(Set.of(doctorRole))
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(doctorRole));
        when(passwordEncoder.encode("securePass123")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.register(validRequest);

        // Assert — capture the saved entity and verify raw password is NOT stored
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$encodedHash");
        assertThat(userCaptor.getValue().getPasswordHash()).doesNotContain("securePass123");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register() — Failure Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() — should throw EmailAlreadyExistsException for duplicate email")
    void register_shouldThrowWhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(validRequest.email())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("john.doe@clinic.com");

        // Verify nothing was saved
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("register() — should throw RoleNotFoundException when role not seeded in DB")
    void register_shouldThrowWhenRoleNotFound() {
        // Arrange
        when(userRepository.existsByEmail(validRequest.email())).thenReturn(false);
        when(roleRepository.findByName(RoleType.DOCTOR)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("DOCTOR");

        // Verify nothing was saved
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadUserByUsername() — UserDetailsService
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername() — should return UserDetails for known email")
    void loadUserByUsername_shouldReturnUserDetails() {
        // Arrange
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email("john.doe@clinic.com")
                .passwordHash("$2a$10$hashed")
                .isEnabled(true)
                .roles(Set.of(doctorRole))
                .build();

        when(userRepository.findByEmail("john.doe@clinic.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails result = authService.loadUserByUsername("john.doe@clinic.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john.doe@clinic.com");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_DOCTOR");
    }

    @Test
    @DisplayName("loadUserByUsername() — should throw UsernameNotFoundException for unknown email")
    void loadUserByUsername_shouldThrowForUnknownEmail() {
        // Arrange
        when(userRepository.findByEmail("ghost@clinic.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.loadUserByUsername("ghost@clinic.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@clinic.com");
    }
}
