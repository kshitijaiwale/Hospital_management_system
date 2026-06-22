package com.hospital.app.appointment.controller;

import com.hospital.app.appointment.dto.request.CreateAppointmentRequest;
import com.hospital.app.appointment.dto.request.RescheduleAppointmentRequest;
import com.hospital.app.appointment.dto.response.AppointmentResponse;
import com.hospital.app.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @PutMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request
    ) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(appointmentId, request));
    }

    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    @PutMapping("/{appointmentId}/missed")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> markMissed(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.markMissed(appointmentId));
    }

    @PutMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<AppointmentResponse> markCompleted(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.markCompleted(appointmentId));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAppointmentsForPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsForPatient(patientId));
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getTodaysAppointments() {
        return ResponseEntity.ok(appointmentService.getTodaysAppointments());
    }
}
