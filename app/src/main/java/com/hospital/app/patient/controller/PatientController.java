package com.hospital.app.patient.controller;

import com.hospital.app.patient.dto.request.CreatePatientRequest;
import com.hospital.app.patient.dto.request.UpdatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.service.PatientRegistrationService;
import com.hospital.app.patient.service.PatientService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final PatientRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<PatientResponse> registerPatient(
            @Valid @RequestBody CreatePatientRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.registerPatient(request));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or @patientSecurity.isOwner(#patientId)")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable UUID patientId) {
        return ResponseEntity.ok(patientService.getPatientById(patientId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    public ResponseEntity<List<PatientResponse>> searchPatients(@RequestParam("query") String query) {
        return ResponseEntity.ok(patientService.searchPatients(query));
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasRole('RECEPTIONIST') or @patientSecurity.isOwner(#patientId)")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable UUID patientId,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        return ResponseEntity.ok(patientService.updatePatient(patientId, request));
    }
}
