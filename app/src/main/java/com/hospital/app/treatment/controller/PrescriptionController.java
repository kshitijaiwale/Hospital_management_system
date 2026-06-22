package com.hospital.app.treatment.controller;

import com.hospital.app.treatment.dto.request.AddPrescriptionsRequest;
import com.hospital.app.treatment.dto.response.PrescriptionResponse;
import com.hospital.app.treatment.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PrescriptionResponse>> addPrescriptions(
            @Valid @RequestBody AddPrescriptionsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.addPrescriptions(request));
    }

    @GetMapping("/consultations/{consultationId}/prescriptions")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsForConsultation(@PathVariable UUID consultationId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsForConsultation(consultationId));
    }
}
