package com.hospital.app.reporting.controller;

import com.hospital.app.reporting.dto.TimelineResponse;
import com.hospital.app.reporting.service.PatientTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PatientTimelineController {

    private final PatientTimelineService patientTimelineService;

    @GetMapping("/patients/{patientId}/timeline")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<TimelineResponse> getPatientTimeline(@PathVariable UUID patientId) {
        return ResponseEntity.ok(patientTimelineService.getPatientTimeline(patientId));
    }
}
