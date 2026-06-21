package com.hospital.app.patient.service.impl;

import com.hospital.app.exception.EmailAlreadyExistsException;
import com.hospital.app.patient.dto.request.CreatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.enums.BloodGroup;
import com.hospital.app.patient.mapper.PatientMapper;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.RoleType;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.RoleRepository;
import com.hospital.app.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientRegistrationServiceImplTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientRegistrationServiceImpl registrationService;

    private CreatePatientRequest request;

    @BeforeEach
    void setUp() {
        request = CreatePatientRequest.builder()
                .name("Jane Doe")
                .email("jane.doe@clinic.com")
                .password("secure123")
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .bloodGroup(BloodGroup.O_POS)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new patient and create user identity")
    void registerPatient_success() {
        Role role = Role.builder().name(RoleType.PATIENT).build();
        User savedUser = User.builder().userId(UUID.randomUUID()).email("jane.doe@clinic.com").build();
        Patient savedPatient = Patient.builder().patientId(UUID.randomUUID()).patientNumber("PAT-2026-000001").build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleType.PATIENT)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(patientRepository.findMaxPatientNumberSequence()).thenReturn(null);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);
        when(patientMapper.toResponse(savedPatient)).thenReturn(PatientResponse.builder().patientNumber("PAT-2026-000001").build());

        PatientResponse response = registrationService.registerPatient(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should fail if email already exists")
    void registerPatient_failEmailExists() {
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> registrationService.registerPatient(request));

        verify(userRepository, never()).save(any());
        verify(patientRepository, never()).save(any());
    }
}
