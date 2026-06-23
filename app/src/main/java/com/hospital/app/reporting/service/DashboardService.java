package com.hospital.app.reporting.service;

import com.hospital.app.reporting.dto.AdminDashboardResponse;
import com.hospital.app.reporting.dto.DoctorDashboardResponse;
import com.hospital.app.reporting.dto.ReceptionistDashboardResponse;

public interface DashboardService {
    DoctorDashboardResponse getDoctorDashboard();
    ReceptionistDashboardResponse getReceptionistDashboard();
    AdminDashboardResponse getAdminDashboard();
}
