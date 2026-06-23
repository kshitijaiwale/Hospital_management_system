package com.hospital.app.reporting.service.impl;

import com.hospital.app.reporting.dto.AdminDashboardResponse;
import com.hospital.app.reporting.dto.DoctorDashboardResponse;
import com.hospital.app.reporting.dto.ReceptionistDashboardResponse;
import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.repository.UserRepository;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TreatmentCaseRepository treatmentCaseRepository;
    @Mock private ConsultationRepository consultationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private DocumentRepository documentRepository;

    @InjectMocks private DashboardServiceImpl dashboardService;

    @Test
    void getDoctorDashboard_success() {
        when(treatmentCaseRepository.countByStatus(any())).thenReturn(5L);
        when(appointmentRepository.countByAppointmentDateTimeBetween(any(), any())).thenReturn(10L);
        when(consultationRepository.countByConsultationDateBetween(any(), any())).thenReturn(8L);

        DoctorDashboardResponse res = dashboardService.getDoctorDashboard();
        assertNotNull(res);
    }

    @Test
    void getReceptionistDashboard_success() {
        when(patientRepository.count()).thenReturn(100L);
        when(appointmentRepository.countByAppointmentDateTimeBetween(any(), any())).thenReturn(15L);

        ReceptionistDashboardResponse res = dashboardService.getReceptionistDashboard();
        assertNotNull(res);
    }

    @Test
    void getAdminDashboard_success() {
        when(userRepository.count()).thenReturn(10L);
        when(patientRepository.count()).thenReturn(100L);
        when(appointmentRepository.count()).thenReturn(50L);
        when(treatmentCaseRepository.count()).thenReturn(30L);
        when(documentRepository.count()).thenReturn(20L);

        AdminDashboardResponse res = dashboardService.getAdminDashboard();
        assertNotNull(res);
    }
}
