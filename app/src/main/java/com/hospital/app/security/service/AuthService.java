package com.hospital.app.security.service;

import com.hospital.app.security.dto.request.LoginRequest;
import com.hospital.app.security.dto.request.RegisterRequest;
import com.hospital.app.security.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Registers a new user in the system.
     * - Staff (ADMIN, DOCTOR, RECEPTIONIST) created by ADMIN via /api/v1/users
     * - PATIENT onboarding is handled separately through the Patient module
     *
     * @param request validated registration payload
     * @return AuthResponse containing user details and success message
     * @throws com.hospital.app.security.exception.EmailAlreadyExistsException if email is taken
     * @throws com.hospital.app.security.exception.RoleNotFoundException        if role not seeded
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user and generates a JWT.
     *
     * @param request validated login payload
     * @return AuthResponse containing user details and token
     */
    AuthResponse login(LoginRequest request);
}
