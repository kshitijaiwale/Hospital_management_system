package com.hospital.app.treatment.controller;

import com.hospital.app.treatment.dto.request.CreateTreatmentCaseRequest;
import com.hospital.app.treatment.dto.response.TreatmentCaseResponse;
import com.hospital.app.treatment.service.TreatmentCaseService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TreatmentCaseController {

    private final TreatmentCaseService treatmentCaseService;

    @PostMapping("/treatment-cases")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<TreatmentCaseResponse> createTreatmentCase(
            @Valid @RequestBody CreateTreatmentCaseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treatmentCaseService.createTreatmentCase(request));
    }

    @PutMapping("/treatment-cases/{caseId}/close")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<TreatmentCaseResponse> closeTreatmentCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(treatmentCaseService.closeTreatmentCase(caseId));
    }

    @GetMapping("/treatment-cases/{caseId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<TreatmentCaseResponse> getTreatmentCaseById(@PathVariable UUID caseId) {
        return ResponseEntity.ok(treatmentCaseService.getTreatmentCaseById(caseId));
    }

    @GetMapping("/patients/{patientId}/treatment-cases")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<List<TreatmentCaseResponse>> getTreatmentCasesForPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(treatmentCaseService.getTreatmentCasesForPatient(patientId));
    }
}
