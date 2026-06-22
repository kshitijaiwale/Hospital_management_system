package com.hospital.app.treatment.service.impl;

import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.treatment.dto.request.CreateTreatmentCaseRequest;
import com.hospital.app.treatment.dto.response.TreatmentCaseResponse;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.mapper.TreatmentCaseMapper;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentCaseServiceImplTest {

    @Mock private TreatmentCaseRepository treatmentCaseRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private TreatmentCaseMapper treatmentCaseMapper;

    @InjectMocks private TreatmentCaseServiceImpl treatmentCaseService;

    private UUID patientId;
    private UUID caseId;
    private Patient patient;
    private TreatmentCase treatmentCase;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        patient = Patient.builder().patientId(patientId).build();
        treatmentCase = TreatmentCase.builder()
                .treatmentCaseId(caseId)
                .patient(patient)
                .status(CaseStatus.ACTIVE)
                .openDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createTreatmentCase_success() {
        CreateTreatmentCaseRequest request = CreateTreatmentCaseRequest.builder()
                .patientId(patientId)
                .title("Fever")
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(treatmentCaseRepository.save(any(TreatmentCase.class))).thenReturn(treatmentCase);
        when(treatmentCaseMapper.toResponse(any())).thenReturn(TreatmentCaseResponse.builder().treatmentCaseId(caseId).status(CaseStatus.ACTIVE).build());

        TreatmentCaseResponse response = treatmentCaseService.createTreatmentCase(request);

        assertNotNull(response);
        assertEquals(CaseStatus.ACTIVE, response.status());
    }

    @Test
    void createTreatmentCase_patientNotFound() {
        CreateTreatmentCaseRequest request = CreateTreatmentCaseRequest.builder().patientId(patientId).build();
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> treatmentCaseService.createTreatmentCase(request));
    }

    @Test
    void closeTreatmentCase_success() {
        when(treatmentCaseRepository.findById(caseId)).thenReturn(Optional.of(treatmentCase));
        when(treatmentCaseRepository.save(any(TreatmentCase.class))).thenReturn(treatmentCase);
        when(treatmentCaseMapper.toResponse(any())).thenReturn(TreatmentCaseResponse.builder().status(CaseStatus.CLOSED).build());

        TreatmentCaseResponse response = treatmentCaseService.closeTreatmentCase(caseId);

        assertEquals(CaseStatus.CLOSED, response.status());
        assertEquals(CaseStatus.CLOSED, treatmentCase.getStatus());
        assertNotNull(treatmentCase.getCloseDate());
    }

    @Test
    void closeTreatmentCase_alreadyClosed() {
        treatmentCase.setStatus(CaseStatus.CLOSED);
        when(treatmentCaseRepository.findById(caseId)).thenReturn(Optional.of(treatmentCase));

        assertThrows(InvalidCaseStateException.class, () -> treatmentCaseService.closeTreatmentCase(caseId));
    }
}
