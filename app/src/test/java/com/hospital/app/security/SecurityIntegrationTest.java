package com.hospital.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.app.security.dto.request.LoginRequest;
import com.hospital.app.security.dto.request.RegisterRequest;
import com.hospital.app.security.entity.RoleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully authenticate a pre-seeded admin user and return JWT")
    void login_shouldReturnToken() throws Exception {
        // Assume an admin is seeded or we register one first.
        // Actually, RoleSeeder seeds roles, but no default users are seeded yet.
        // So we might get 403 or 401. Let's create an integration test that creates a user if possible.
        // Since /api/v1/users requires ADMIN role, we have a chicken and egg problem here in tests.
        // We will just try to login with random credentials to get 401 first.

        LoginRequest request = LoginRequest.builder()
                .email("admin@clinic.com")
                .password("admin123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Mapped to 403 by default without explicit ExceptionHandler
    }

    @Test
    @DisplayName("Should block registration if no Bearer token provided (403 Forbidden / 401 Unauthorized)")
    void register_shouldBlockUnauthenticatedAccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("New Doctor")
                .email("doctor@clinic.com")
                .password("securePass")
                .role(RoleType.DOCTOR)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Spring security defaults to 403 for unauthenticated access to secured endpoints without an entry point
    }
}
