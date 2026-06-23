package com.hospital.app.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardResponse {
    private long totalActiveTreatmentCases;
    private long todayAppointments;
    private long todayConsultations;
}
