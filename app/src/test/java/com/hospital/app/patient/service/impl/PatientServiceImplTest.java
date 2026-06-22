package com.hospital.app.patient.service.impl;

import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.dto.request.UpdatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.enums.BloodGroup;
import com.hospital.app.patient.enums.PatientStatus;
import com.hospital.app.patient.mapper.PatientMapper;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private UUID patientId;
    private Patient patient;
    private User user;
    private PatientResponse patientResponse;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        user = User.builder()
                .userId(UUID.randomUUID())
                .name("John Doe")
                .email("john.doe@clinic.com")
                .build();

        patient = Patient.builder()
                .patientId(patientId)
                .patientNumber("PAT-2026-000001")
                .user(user)
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .bloodGroup(BloodGroup.O_POS)
                .status(PatientStatus.ACTIVE)
                .build();

        patientResponse = PatientResponse.builder()
                .patientId(patientId)
                .userId(user.getUserId())
                .patientNumber("PAT-2026-000001")
                .name("John Doe")
                .email("john.doe@clinic.com")
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .bloodGroup(BloodGroup.O_POS)
                .status(PatientStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should successfully retrieve a patient by ID")
    void getPatientById_success() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(patientResponse);

        PatientResponse result = patientService.getPatientById(patientId);

        assertNotNull(result);
        assertEquals(patientId, result.patientId());
        assertEquals("John Doe", result.name());
        verify(patientRepository).findById(patientId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patient ID does not exist")
    void getPatientById_notFound() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getPatientById(patientId));
        verify(patientRepository).findById(patientId);
        verify(patientMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return matching patients when searching")
    void searchPatients_success() {
        String query = "John";
        when(patientRepository.searchPatients(query)).thenReturn(List.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(patientResponse);

        List<PatientResponse> results = patientService.searchPatients(query);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("John Doe", results.get(0).name());
        verify(patientRepository).searchPatients(query);
    }

    @Test
    @DisplayName("Should successfully update a patient's details")
    void updatePatient_success() {
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .phone("0987654321")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .bloodGroup(BloodGroup.A_NEG)
                .status(PatientStatus.ACTIVE)
                .address("123 Health St")
                .emergencyContactName("Mary Doe")
                .emergencyContactPhone("9876543210")
                .build();

        Patient updatedPatient = Patient.builder()
                .patientId(patientId)
                .patientNumber("PAT-2026-000001")
                .user(user)
                .phone("0987654321")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .bloodGroup(BloodGroup.A_NEG)
                .status(PatientStatus.ACTIVE)
                .address("123 Health St")
                .emergencyContactName("Mary Doe")
                .emergencyContactPhone("9876543210")
                .build();

        PatientResponse updatedResponse = PatientResponse.builder()
                .patientId(patientId)
                .userId(user.getUserId())
                .patientNumber("PAT-2026-000001")
                .name("John Doe")
                .email("john.doe@clinic.com")
                .phone("0987654321")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .bloodGroup(BloodGroup.A_NEG)
                .status(PatientStatus.ACTIVE)
                .address("123 Health St")
                .emergencyContactName("Mary Doe")
                .emergencyContactPhone("9876543210")
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(updatedPatient);
        when(patientMapper.toResponse(updatedPatient)).thenReturn(updatedResponse);

        PatientResponse result = patientService.updatePatient(patientId, updateRequest);

        assertNotNull(result);
        assertEquals("0987654321", result.phone());
        assertEquals(BloodGroup.A_NEG, result.bloodGroup());
        assertEquals("123 Health St", result.address());
        verify(patientRepository).findById(patientId);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent patient")
    void updatePatient_notFound() {
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .phone("0987654321")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .bloodGroup(BloodGroup.A_NEG)
                .status(PatientStatus.ACTIVE)
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.updatePatient(patientId, updateRequest));
        verify(patientRepository).findById(patientId);
        verify(patientRepository, never()).save(any());
    }
}
