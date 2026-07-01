package com.hospital.app.queue.controller;

import com.hospital.app.queue.dto.response.PatientQueueStatusResponse;
import com.hospital.app.queue.dto.response.QueueEntryResponse;
import com.hospital.app.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Queue Management API.
 *
 * Two distinct surfaces:
 *  1. /api/v1/queue/**        — Receptionist-operated (write + read)
 *  2. /api/v1/queue/my-status — Patient-facing (read-only, their appointment only)
 */
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    // ── Receptionist: write operations ───────────────────────────────────────

    /**
     * POST /api/v1/queue/appointments/{appointmentId}/checkin
     * Receptionist checks a patient in and issues a queue token.
     */
    @PostMapping("/appointments/{appointmentId}/checkin")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> checkIn(@PathVariable UUID appointmentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.checkIn(appointmentId));
    }

    /**
     * POST /api/v1/queue/{queueEntryId}/start-consultation
     * Receptionist calls the patient in — marks consultation as started.
     */
    @PostMapping("/{queueEntryId}/start-consultation")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> startConsultation(@PathVariable UUID queueEntryId) {
        return ResponseEntity.ok(queueService.startConsultation(queueEntryId));
    }

    /**
     * POST /api/v1/queue/{queueEntryId}/complete-consultation
     * Receptionist (or doctor via receptionist) marks consultation complete.
     */
    @PostMapping("/{queueEntryId}/complete-consultation")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> completeConsultation(@PathVariable UUID queueEntryId) {
        return ResponseEntity.ok(queueService.completeConsultation(queueEntryId));
    }

    /**
     * POST /api/v1/queue/{queueEntryId}/no-show
     * Receptionist marks a waiting patient as no-show.
     */
    @PostMapping("/{queueEntryId}/no-show")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> markNoShow(@PathVariable UUID queueEntryId) {
        return ResponseEntity.ok(queueService.markNoShow(queueEntryId));
    }

    /**
     * POST /api/v1/queue/{queueEntryId}/cancel
     * Receptionist cancels a queue entry.
     */
    @PostMapping("/{queueEntryId}/cancel")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> cancelQueueEntry(@PathVariable UUID queueEntryId) {
        return ResponseEntity.ok(queueService.cancelQueueEntry(queueEntryId));
    }

    // ── Receptionist: read operations ─────────────────────────────────────────

    /**
     * GET /api/v1/queue/today
     * Full queue for today — all statuses, ordered by token number.
     */
    @GetMapping("/today")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<QueueEntryResponse>> getTodaysQueue() {
        return ResponseEntity.ok(queueService.getTodaysQueue());
    }

    /**
     * GET /api/v1/queue/current
     * Which patient is currently in consultation.
     */
    @GetMapping("/current")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> getCurrentlyServing() {
        return ResponseEntity.ok(queueService.getCurrentlyServing());
    }

    /**
     * GET /api/v1/queue/next
     * The next WAITING patient to be called.
     */
    @GetMapping("/next")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<QueueEntryResponse> getNextInQueue() {
        return ResponseEntity.ok(queueService.getNextInQueue());
    }

    // ── Patient-facing: read-only ─────────────────────────────────────────────

    /**
     * GET /api/v1/queue/my-status/{appointmentId}
     * Patient checks their own queue position and current serving number.
     * Patient can only access their own appointment (enforced downstream via security).
     */
    @GetMapping("/my-status/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<PatientQueueStatusResponse> getMyQueueStatus(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(queueService.getMyQueueStatus(appointmentId));
    }
}