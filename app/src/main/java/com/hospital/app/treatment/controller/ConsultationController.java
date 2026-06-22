package com.hospital.app.treatment.controller;

import com.hospital.app.treatment.dto.request.CreateConsultationRequest;
import com.hospital.app.treatment.dto.request.UpdateConsultationNotesRequest;
import com.hospital.app.treatment.dto.response.ConsultationResponse;
import com.hospital.app.treatment.service.ConsultationService;
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
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping("/consultations")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ConsultationResponse> createConsultation(
            @Valid @RequestBody CreateConsultationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultationService.createConsultation(request));
    }

    @PutMapping("/consultations/{consultationId}/notes")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ConsultationResponse> addClinicalNotes(
            @PathVariable UUID consultationId,
            @Valid @RequestBody UpdateConsultationNotesRequest request
    ) {
        return ResponseEntity.ok(consultationService.addClinicalNotes(consultationId, request));
    }

    @GetMapping("/consultations/{consultationId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ConsultationResponse> getConsultationById(@PathVariable UUID consultationId) {
        return ResponseEntity.ok(consultationService.getConsultationById(consultationId));
    }
    
    @GetMapping("/treatment-cases/{caseId}/consultations")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<ConsultationResponse>> getConsultationsForCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(consultationService.getConsultationsForCase(caseId));
    }
}
