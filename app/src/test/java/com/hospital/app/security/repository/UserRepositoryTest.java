package com.hospital.app.security.repository;

import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.RoleType;
import com.hospital.app.security.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        // Seed a PATIENT role
        Role patientRole = roleRepository.save(
                Role.builder().name(RoleType.PATIENT).build()
        );

        // Build and persist a test user
        User user = User.builder()
                .name("John Doe")
                .email("john.doe@clinic.com")
                .passwordHash("$2a$10$hashedpassword")
                .isEnabled(true)
                .roles(Set.of(patientRole))
                .build();

        savedUser = userRepository.save(user);
    }

    @Test
    @DisplayName("Should save user and auto-generate UUID")
    void shouldSaveUserWithGeneratedId() {
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        Optional<User> found = userRepository.findByEmail("john.doe@clinic.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john.doe@clinic.com");
    }

    @Test
    @DisplayName("Should return empty Optional for unknown email")
    void shouldReturnEmptyForUnknownEmail() {
        Optional<User> found = userRepository.findByEmail("unknown@clinic.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return true when email already exists")
    void shouldReturnTrueForExistingEmail() {
        boolean exists = userRepository.existsByEmail("john.doe@clinic.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseForNewEmail() {
        boolean exists = userRepository.existsByEmail("new.user@clinic.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should map roles correctly via user_role join table")
    void shouldLoadRolesEagerly() {
        User found = userRepository.findByEmail("john.doe@clinic.com").orElseThrow();

        assertThat(found.getRoles()).hasSize(1);
        assertThat(found.getRoles().iterator().next().getName()).isEqualTo(RoleType.PATIENT);
    }

    @Test
    @DisplayName("Should return ROLE_PATIENT as Spring Security authority")
    void shouldReturnCorrectGrantedAuthority() {
        User found = userRepository.findByEmail("john.doe@clinic.com").orElseThrow();

        assertThat(found.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PATIENT");
    }
}
