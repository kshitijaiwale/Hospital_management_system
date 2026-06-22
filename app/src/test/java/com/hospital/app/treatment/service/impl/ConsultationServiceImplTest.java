package com.hospital.app.treatment.service.impl;

import com.hospital.app.appointment.entity.Appointment;
import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.appointment.service.AppointmentService;
import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.UserRepository;
import com.hospital.app.treatment.dto.request.CreateConsultationRequest;
import com.hospital.app.treatment.dto.response.ConsultationResponse;
import com.hospital.app.treatment.entity.Consultation;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.mapper.ConsultationMapper;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceImplTest {

    @Mock private ConsultationRepository consultationRepository;
    @Mock private TreatmentCaseRepository treatmentCaseRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentService appointmentService;
    @Mock private UserRepository userRepository;
    @Mock private ConsultationMapper consultationMapper;

    @InjectMocks private ConsultationServiceImpl consultationService;

    private UUID caseId;
    private UUID appointmentId;
    private TreatmentCase treatmentCase;
    private Appointment appointment;
    private User doctor;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
        treatmentCase = TreatmentCase.builder().treatmentCaseId(caseId).status(CaseStatus.ACTIVE).build();
        appointment = Appointment.builder().appointmentId(appointmentId).build();
        doctor = User.builder().userId(UUID.randomUUID()).email("doc@test.com").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("doc@test.com", "password", java.util.Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createConsultation_successWithAppointment() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("doc@test.com", "password", java.util.Collections.emptyList())
        );

        CreateConsultationRequest request = CreateConsultationRequest.builder()
                .treatmentCaseId(caseId)
                .appointmentId(appointmentId)
                .build();

        when(treatmentCaseRepository.findById(caseId)).thenReturn(Optional.of(treatmentCase));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail("doc@test.com")).thenReturn(Optional.of(doctor));
        when(consultationRepository.save(any(Consultation.class))).thenReturn(new Consultation());
        when(consultationMapper.toResponse(any())).thenReturn(ConsultationResponse.builder().build());

        ConsultationResponse response = consultationService.createConsultation(request);

        assertNotNull(response);
        verify(appointmentService).markCompleted(appointmentId);
    }

    @Test
    void createConsultation_closedCase() {
        treatmentCase.setStatus(CaseStatus.CLOSED);
        CreateConsultationRequest request = CreateConsultationRequest.builder().treatmentCaseId(caseId).build();

        when(treatmentCaseRepository.findById(caseId)).thenReturn(Optional.of(treatmentCase));

        assertThrows(InvalidCaseStateException.class, () -> consultationService.createConsultation(request));
    }
}
