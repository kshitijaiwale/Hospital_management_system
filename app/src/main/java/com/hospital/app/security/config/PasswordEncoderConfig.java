package com.hospital.app.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Isolated PasswordEncoder configuration.
 *
 * IMPORTANT: This MUST be kept separate from SecurityConfig.
 * Placing BCryptPasswordEncoder inside SecurityConfig creates a circular
 * dependency at Spring context startup:
 *   SecurityConfig → UserDetailsService → AuthServiceImpl → PasswordEncoder → SecurityConfig
 * Extracting it here breaks that cycle cleanly.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
