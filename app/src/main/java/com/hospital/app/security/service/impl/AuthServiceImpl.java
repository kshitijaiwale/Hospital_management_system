package com.hospital.app.security.service.impl;

import com.hospital.app.security.dto.request.RegisterRequest;
import com.hospital.app.security.dto.response.AuthResponse;
import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.exception.EmailAlreadyExistsException;
import com.hospital.app.security.exception.RoleNotFoundException;
import com.hospital.app.security.repository.RoleRepository;
import com.hospital.app.security.repository.UserRepository;
import com.hospital.app.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import com.hospital.app.security.dto.request.LoginRequest;
import com.hospital.app.security.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.context.annotation.Lazy;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Lazy AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed — email already exists: {}", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new RoleNotFoundException(request.role()));

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isEnabled(true)
                .roles(Set.of(role))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: id={}, email={}, role={}",
                savedUser.getUserId(), savedUser.getEmail(), role.getName());

        return new AuthResponse(
                savedUser.getUserId(),
                savedUser.getName(),
                savedUser.getEmail(),
                role.getName().name(),
                null,
                "User registered successfully"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.email());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);
        String role = user.getRoles().iterator().next().getName().name();

        return new AuthResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                role,
                token,
                "Login successful"
        );
    }

    /**
     * Used by Spring Security JwtAuthenticationFilter (wired in next phase).
     * Loads the full User entity (which implements UserDetails) by email.
     *
     * @throws UsernameNotFoundException if email not found — Spring Security expects this type
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));
    }
}
