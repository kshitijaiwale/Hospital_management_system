package com.hospital.app.security.repository;

import com.hospital.app.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Used during login / JWT authentication / UserDetailsService lookup.
     */
    Optional<User> findByEmail(String email);

    /**
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);
}
