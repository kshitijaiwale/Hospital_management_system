package com.hospital.app.reporting.service.impl;

import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.reporting.dto.TimelineEvent;
import com.hospital.app.reporting.dto.TimelineResponse;
import com.hospital.app.reporting.service.PatientTimelineService;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientTimelineServiceImpl implements PatientTimelineService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TreatmentCaseRepository treatmentCaseRepository;
    private final ConsultationRepository consultationRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public TimelineResponse getPatientTimeline(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found");
        }

        List<TimelineEvent> events = new ArrayList<>();

        // 1. Appointments
        appointmentRepository.findByPatientPatientIdOrderByAppointmentDateTimeDesc(patientId).forEach(appt -> {
            events.add(TimelineEvent.builder()
                    .eventType("APPOINTMENT")
                    .entityId(appt.getAppointmentId())
                    .eventDate(appt.getAppointmentDateTime())
                    .title("Appointment Scheduled")
                    .description(appt.getNotes() != null ? appt.getNotes() : "Routine appointment")
                    .status(appt.getStatus() != null ? appt.getStatus().name() : "SCHEDULED")
                    .build());
        });

        // 2. Treatment Cases
        treatmentCaseRepository.findByPatientPatientIdOrderByOpenDateDesc(patientId).forEach(tc -> {
            events.add(TimelineEvent.builder()
                    .eventType("TREATMENT_CASE")
                    .entityId(tc.getTreatmentCaseId())
                    .eventDate(tc.getOpenDate())
                    .title("Treatment Case Opened: " + tc.getTitle())
                    .description("Diagnosis: " + tc.getDiagnosis())
                    .status(tc.getStatus() != null ? tc.getStatus().name() : "ACTIVE")
                    .build());
        });

        // 3. Consultations
        consultationRepository.findByTreatmentCasePatientPatientIdOrderByConsultationDateDesc(patientId).forEach(con -> {
            events.add(TimelineEvent.builder()
                    .eventType("CONSULTATION")
                    .entityId(con.getConsultationId())
                    .eventDate(con.getConsultationDate())
                    .title("Consultation")
                    .description("Symptoms: " + con.getSymptoms() + ". Notes: " + con.getClinicalNotes())
                    .performedBy(con.getDoctor() != null ? con.getDoctor().getName() : null)
                    .build());
        });

        // 4. Documents
        documentRepository.findByPatientPatientIdOrderByUploadedAtDesc(patientId).forEach(doc -> {
            events.add(TimelineEvent.builder()
                    .eventType("DOCUMENT")
                    .entityId(doc.getDocumentId())
                    .eventDate(doc.getUploadedAt())
                    .title("Document Uploaded: " + doc.getDocumentType().name())
                    .description("File: " + doc.getFileName())
                    .performedBy(doc.getUploadedBy() != null ? doc.getUploadedBy().getName() : null)
                    .build());
        });

        // Sort events chronologically (newest first)
        events.sort(Comparator.comparing(TimelineEvent::getEventDate).reversed());

        return TimelineResponse.builder()
                .patientId(patientId)
                .events(events)
                .build();
    }
}
