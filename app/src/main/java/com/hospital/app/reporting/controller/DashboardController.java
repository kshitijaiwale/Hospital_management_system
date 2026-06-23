package com.hospital.app.reporting.controller;

import com.hospital.app.reporting.dto.AdminDashboardResponse;
import com.hospital.app.reporting.dto.DoctorDashboardResponse;
import com.hospital.app.reporting.dto.ReceptionistDashboardResponse;
import com.hospital.app.reporting.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorDashboardResponse> getDoctorDashboard() {
        return ResponseEntity.ok(dashboardService.getDoctorDashboard());
    }

    @GetMapping("/receptionist")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<ReceptionistDashboardResponse> getReceptionistDashboard() {
        return ResponseEntity.ok(dashboardService.getReceptionistDashboard());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }
}
