package com.hospital.app.reporting.service.impl;

import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.reporting.dto.AdminDashboardResponse;
import com.hospital.app.reporting.dto.DoctorDashboardResponse;
import com.hospital.app.reporting.dto.ReceptionistDashboardResponse;
import com.hospital.app.reporting.service.DashboardService;
import com.hospital.app.security.repository.UserRepository;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final TreatmentCaseRepository treatmentCaseRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public DoctorDashboardResponse getDoctorDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return DoctorDashboardResponse.builder()
                .totalActiveTreatmentCases(treatmentCaseRepository.countByStatus(CaseStatus.ACTIVE))
                .todayAppointments(appointmentRepository.countByAppointmentDateTimeBetween(startOfDay, endOfDay))
                .todayConsultations(consultationRepository.countByConsultationDateBetween(startOfDay, endOfDay))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionistDashboardResponse getReceptionistDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return ReceptionistDashboardResponse.builder()
                .totalPatients(patientRepository.count())
                .todayAppointments(appointmentRepository.countByAppointmentDateTimeBetween(startOfDay, endOfDay))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalPatients(patientRepository.count())
                .totalAppointments(appointmentRepository.count())
                .totalTreatmentCases(treatmentCaseRepository.count())
                .totalDocuments(documentRepository.count())
                .build();
    }
}
