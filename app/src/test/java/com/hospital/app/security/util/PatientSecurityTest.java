package com.hospital.app.security.util;

import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientSecurityTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PatientSecurity patientSecurity;

    private UUID patientId;
    private Patient patient;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email("patient@clinic.com")
                .build();
        patient = Patient.builder()
                .patientId(patientId)
                .user(user)
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return true when logged-in user email matches patient user email")
    void isOwner_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("patient@clinic.com");
        when(authentication.getName()).thenReturn("patient@clinic.com");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        boolean result = patientSecurity.isOwner(patientId);

        assertTrue(result);
        verify(patientRepository).findById(patientId);
    }

    @Test
    @DisplayName("Should return false when logged-in user email does not match patient user email")
    void isOwner_notMatchingEmail() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("other@clinic.com");
        when(authentication.getName()).thenReturn("other@clinic.com");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        boolean result = patientSecurity.isOwner(patientId);

        assertFalse(result);
        verify(patientRepository).findById(patientId);
    }

    @Test
    @DisplayName("Should return false when authentication is null")
    void isOwner_nullAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = patientSecurity.isOwner(patientId);

        assertFalse(result);
        verify(patientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return false when not authenticated")
    void isOwner_notAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result = patientSecurity.isOwner(patientId);

        assertFalse(result);
        verify(patientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return false when principal is anonymousUser")
    void isOwner_anonymousUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        boolean result = patientSecurity.isOwner(patientId);

        assertFalse(result);
        verify(patientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return false when patient not found")
    void isOwner_patientNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("patient@clinic.com");
        when(authentication.getName()).thenReturn("patient@clinic.com");
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        boolean result = patientSecurity.isOwner(patientId);

        assertFalse(result);
        verify(patientRepository).findById(patientId);
    }
}
