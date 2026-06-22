package com.hospital.app.treatment.service.impl;

import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.treatment.dto.request.AddPrescriptionsRequest;
import com.hospital.app.treatment.dto.request.CreatePrescriptionRequest;
import com.hospital.app.treatment.dto.response.PrescriptionResponse;
import com.hospital.app.treatment.entity.Consultation;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.mapper.PrescriptionMapper;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.PrescriptionRepository;
import com.hospital.app.treatment.entity.Prescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private ConsultationRepository consultationRepository;
    @Mock private PrescriptionMapper prescriptionMapper;

    @InjectMocks private PrescriptionServiceImpl prescriptionService;

    private UUID consultationId;
    private Consultation consultation;
    private TreatmentCase treatmentCase;

    @BeforeEach
    void setUp() {
        consultationId = UUID.randomUUID();
        treatmentCase = TreatmentCase.builder().status(CaseStatus.ACTIVE).build();
        consultation = Consultation.builder().consultationId(consultationId).treatmentCase(treatmentCase).build();
    }

    @Test
    void addPrescriptions_success() {
        AddPrescriptionsRequest request = AddPrescriptionsRequest.builder()
                .consultationId(consultationId)
                .prescriptions(List.of(
                        CreatePrescriptionRequest.builder().medicationName("A").build(),
                        CreatePrescriptionRequest.builder().medicationName("B").build()
                ))
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(prescriptionRepository.saveAll(anyList())).thenReturn(List.of(new Prescription(), new Prescription()));
        when(prescriptionMapper.toResponse(any())).thenReturn(PrescriptionResponse.builder().build());

        List<PrescriptionResponse> responses = prescriptionService.addPrescriptions(request);

        assertEquals(2, responses.size());
        verify(prescriptionRepository).saveAll(anyList());
    }

    @Test
    void addPrescriptions_closedCase() {
        treatmentCase.setStatus(CaseStatus.CLOSED);
        AddPrescriptionsRequest request = AddPrescriptionsRequest.builder().consultationId(consultationId).prescriptions(List.of()).build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));

        assertThrows(InvalidCaseStateException.class, () -> prescriptionService.addPrescriptions(request));
    }
}
