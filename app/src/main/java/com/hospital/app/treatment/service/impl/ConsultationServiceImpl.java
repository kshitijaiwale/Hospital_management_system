package com.hospital.app.treatment.service.impl;

import com.hospital.app.appointment.entity.Appointment;
import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.appointment.service.AppointmentService;
import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.UserRepository;
import com.hospital.app.treatment.dto.request.CreateConsultationRequest;
import com.hospital.app.treatment.dto.request.UpdateConsultationNotesRequest;
import com.hospital.app.treatment.dto.response.ConsultationResponse;
import com.hospital.app.treatment.entity.Consultation;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.exception.TreatmentNotFoundException;
import com.hospital.app.treatment.mapper.ConsultationMapper;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import com.hospital.app.treatment.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final TreatmentCaseRepository treatmentCaseRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final ConsultationMapper consultationMapper;

    @Override
    @Transactional
    public ConsultationResponse createConsultation(CreateConsultationRequest request) {
        log.info("Creating consultation for treatment case: {}", request.treatmentCaseId());

        TreatmentCase treatmentCase = treatmentCaseRepository.findById(request.treatmentCaseId())
                .orElseThrow(() -> new TreatmentNotFoundException("Treatment Case not found with ID: " + request.treatmentCaseId()));

        if (treatmentCase.getStatus() == CaseStatus.CLOSED) {
            throw new InvalidCaseStateException("Cannot add consultation to a closed treatment case");
        }

        Appointment appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.appointmentId()));
            // Automatically mark appointment as completed
            log.info("Marking appointment {} as COMPLETED", appointment.getAppointmentId());
            appointmentService.markCompleted(appointment.getAppointmentId());
        }

        User doctor = resolveCurrentUser();
        if (doctor == null) {
            throw new IllegalStateException("Current user could not be resolved");
        }

        Consultation consultation = Consultation.builder()
                .treatmentCase(treatmentCase)
                .appointment(appointment)
                .doctor(doctor)
                .consultationDate(LocalDateTime.now())
                .symptoms(request.symptoms())
                .diagnosis(request.diagnosis())
                .clinicalNotes(request.clinicalNotes())
                .recommendations(request.recommendations())
                .build();

        Consultation saved = consultationRepository.save(consultation);
        log.info("Created consultation with ID: {}", saved.getConsultationId());

        return consultationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ConsultationResponse addClinicalNotes(UUID consultationId, UpdateConsultationNotesRequest request) {
        log.info("Adding clinical notes to consultation: {}", consultationId);

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + consultationId));

        if (consultation.getTreatmentCase().getStatus() == CaseStatus.CLOSED) {
            throw new InvalidCaseStateException("Cannot add notes to a consultation of a closed treatment case");
        }

        consultation.setClinicalNotes(request.clinicalNotes());
        Consultation saved = consultationRepository.save(consultation);

        return consultationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(UUID consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + consultationId));
        return consultationMapper.toResponse(consultation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getConsultationsForCase(UUID treatmentCaseId) {
        if (!treatmentCaseRepository.existsById(treatmentCaseId)) {
            throw new TreatmentNotFoundException("Treatment Case not found with ID: " + treatmentCaseId);
        }

        return consultationRepository.findByTreatmentCaseTreatmentCaseIdOrderByConsultationDateDesc(treatmentCaseId)
                .stream()
                .map(consultationMapper::toResponse)
                .collect(Collectors.toList());
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }
}
