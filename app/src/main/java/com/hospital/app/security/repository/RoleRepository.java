package com.hospital.app.security.repository;

import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Used for:
     * - Initial system role seeding (ADMIN, DOCTOR on startup)
     * - Auto-assigning PATIENT role during registration
     * - Role existence validation before assignment
     */
    Optional<Role> findByName(RoleType name);
}
