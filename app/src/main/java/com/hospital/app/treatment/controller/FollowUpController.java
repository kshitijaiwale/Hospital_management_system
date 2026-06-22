package com.hospital.app.treatment.controller;

import com.hospital.app.treatment.dto.request.CreateFollowUpRequest;
import com.hospital.app.treatment.dto.request.UpdateFollowUpRequest;
import com.hospital.app.treatment.dto.response.FollowUpResponse;
import com.hospital.app.treatment.service.FollowUpService;
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
public class FollowUpController {

    private final FollowUpService followUpService;

    @PostMapping("/followups")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<FollowUpResponse> scheduleFollowUp(
            @Valid @RequestBody CreateFollowUpRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followUpService.scheduleFollowUp(request));
    }

    @PutMapping("/followups/{followUpId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    public ResponseEntity<FollowUpResponse> updateFollowUpStatus(
            @PathVariable UUID followUpId,
            @Valid @RequestBody UpdateFollowUpRequest request
    ) {
        return ResponseEntity.ok(followUpService.updateFollowUpStatus(followUpId, request));
    }

    @GetMapping("/treatment-cases/{caseId}/followups")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<List<FollowUpResponse>> getFollowUpsForCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(followUpService.getFollowUpsForCase(caseId));
    }
}
