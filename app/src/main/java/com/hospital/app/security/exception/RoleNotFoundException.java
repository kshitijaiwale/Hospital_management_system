package com.hospital.app.security.exception;

import com.hospital.app.security.entity.RoleType;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(RoleType role) {
        super("Role '" + role.name() + "' not found in the system. Ensure roles are seeded on startup.");
    }
}
