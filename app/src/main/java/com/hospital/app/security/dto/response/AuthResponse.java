package com.hospital.app.security.dto.response;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String name,
        String email,
        String role,
        String token,
        String message
) {}
