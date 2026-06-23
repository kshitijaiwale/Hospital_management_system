package com.hospital.app.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalPatients;
    private long totalAppointments;
    private long totalTreatmentCases;
    private long totalDocuments;
}
