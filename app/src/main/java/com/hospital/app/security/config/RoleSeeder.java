package com.hospital.app.security.config;

import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.RoleType;
import com.hospital.app.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final com.hospital.app.security.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding default roles...");
        Arrays.stream(RoleType.values()).forEach(roleType -> {
            if (roleRepository.findByName(roleType).isEmpty()) {
                roleRepository.save(Role.builder().name(roleType).build());
                log.info("Seeded role: {}", roleType.name());
            }
        });
        log.info("Role seeding completed.");

        if (userRepository.findByEmail("admin@clinic.com").isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleType.ADMIN).get();
            java.util.Set<Role> adminRoles = new java.util.HashSet<>();
            adminRoles.add(adminRole);
            com.hospital.app.security.entity.User adminUser = com.hospital.app.security.entity.User.builder()
                    .email("admin@clinic.com")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .name("System Admin")
                    .roles(adminRoles)
                    .isEnabled(true)
                    .build();
            userRepository.save(adminUser);
            log.info("Seeded default admin user: admin@clinic.com / admin");
        }
    }
}
