package com.hospital.app.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistDashboardResponse {
    private long totalPatients;
    private long todayAppointments;
}
