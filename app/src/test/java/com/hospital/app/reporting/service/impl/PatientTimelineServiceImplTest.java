package com.hospital.app.reporting.service.impl;

import com.hospital.app.appointment.entity.Appointment;
import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.document.entity.PatientDocument;
import com.hospital.app.common.enums.DocumentType;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.reporting.dto.TimelineResponse;
import com.hospital.app.treatment.entity.Consultation;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientTimelineServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TreatmentCaseRepository treatmentCaseRepository;
    @Mock private ConsultationRepository consultationRepository;
    @Mock private DocumentRepository documentRepository;

    @InjectMocks private PatientTimelineServiceImpl patientTimelineService;

    @Test
    void getPatientTimeline_success() {
        UUID patientId = UUID.randomUUID();

        when(patientRepository.existsById(patientId)).thenReturn(true);
        
        Appointment a = new Appointment();
        a.setAppointmentDateTime(LocalDateTime.now().minusDays(1));
        when(appointmentRepository.findByPatientPatientIdOrderByAppointmentDateTimeDesc(patientId))
                .thenReturn(List.of(a));

        TreatmentCase tc = new TreatmentCase();
        tc.setOpenDate(LocalDateTime.now().minusDays(2));
        when(treatmentCaseRepository.findByPatientPatientIdOrderByOpenDateDesc(patientId))
                .thenReturn(List.of(tc));

        Consultation c = new Consultation();
        c.setConsultationDate(LocalDateTime.now().minusDays(3));
        when(consultationRepository.findByTreatmentCasePatientPatientIdOrderByConsultationDateDesc(patientId))
                .thenReturn(List.of(c));

        PatientDocument pd = new PatientDocument();
        pd.setUploadedAt(LocalDateTime.now().minusDays(4));
        pd.setDocumentType(DocumentType.LAB_REPORT);
        when(documentRepository.findByPatientPatientIdOrderByUploadedAtDesc(patientId))
                .thenReturn(List.of(pd));

        TimelineResponse res = patientTimelineService.getPatientTimeline(patientId);

        assertNotNull(res);
        assertEquals(4, res.getEvents().size());
        assertEquals(patientId, res.getPatientId());
    }
}
