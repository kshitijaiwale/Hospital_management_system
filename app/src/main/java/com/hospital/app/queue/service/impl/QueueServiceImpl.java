package com.hospital.app.queue.service.impl;

import com.hospital.app.appointment.entity.Appointment;
import com.hospital.app.appointment.enums.AppointmentStatus;
import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.queue.dto.response.PatientQueueStatusResponse;
import com.hospital.app.queue.dto.response.QueueEntryResponse;
import com.hospital.app.queue.entity.QueueEntry;
import com.hospital.app.queue.enums.QueueStatus;
import com.hospital.app.queue.exception.DuplicateCheckInException;
import com.hospital.app.queue.exception.InvalidQueueStateException;
import com.hospital.app.queue.exception.QueueEntryNotFoundException;
import com.hospital.app.queue.mapper.QueueMapper;
import com.hospital.app.queue.repository.QueueRepository;
import com.hospital.app.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final QueueRepository queueRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueMapper queueMapper;

    // ── Write operations ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public QueueEntryResponse checkIn(UUID appointmentId) {
        log.info("Check-in requested for appointment {}", appointmentId);

        // 1. Validate the appointment exists
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found: " + appointmentId));

        // 2. Only SCHEDULED appointments can be checked in
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidQueueStateException(
                    "Appointment " + appointmentId + " is " + appointment.getStatus()
                            + " — only SCHEDULED appointments can be checked in");
        }

        // 3. Prevent duplicate check-ins
        if (queueRepository.existsByAppointmentAppointmentId(appointmentId)) {
            throw new DuplicateCheckInException(
                    "Appointment " + appointmentId + " has already been checked in");
        }

        // 4. Generate the next sequential token for today
        LocalDate today = LocalDate.now();
        int nextToken = queueRepository.findMaxQueueNumberForDate(today)
                .map(max -> max + 1)
                .orElse(1);

        // 5. Build and persist the queue entry
        QueueEntry entry = QueueEntry.builder()
                .appointment(appointment)
                .queueDate(today)
                .queueNumber(nextToken)
                .status(QueueStatus.WAITING)
                .checkInTime(LocalDateTime.now())
                .build();

        entry = queueRepository.save(entry);
        log.info("Patient checked in — token #{} for date {}", nextToken, today);

        return queueMapper.toResponse(entry, computePosition(entry));
    }

    @Override
    @Transactional
    public QueueEntryResponse startConsultation(UUID queueEntryId) {
        log.info("Start consultation requested for queue entry {}", queueEntryId);

        QueueEntry entry = findEntryOrThrow(queueEntryId);
        assertStatus(entry, QueueStatus.WAITING, "start consultation");

        entry.setStatus(QueueStatus.IN_CONSULTATION);
        entry.setConsultationStartTime(LocalDateTime.now());
        entry = queueRepository.save(entry);

        log.info("Consultation started for token #{}", entry.getQueueNumber());
        return queueMapper.toResponse(entry, null);
    }

    @Override
    @Transactional
    public QueueEntryResponse completeConsultation(UUID queueEntryId) {
        log.info("Complete consultation requested for queue entry {}", queueEntryId);

        QueueEntry entry = findEntryOrThrow(queueEntryId);
        assertStatus(entry, QueueStatus.IN_CONSULTATION, "complete consultation");

        entry.setStatus(QueueStatus.COMPLETED);
        entry.setConsultationEndTime(LocalDateTime.now());
        entry = queueRepository.save(entry);

        log.info("Consultation completed for token #{}", entry.getQueueNumber());
        return queueMapper.toResponse(entry, null);
    }

    @Override
    @Transactional
    public QueueEntryResponse markNoShow(UUID queueEntryId) {
        log.info("No-show requested for queue entry {}", queueEntryId);

        QueueEntry entry = findEntryOrThrow(queueEntryId);
        assertStatus(entry, QueueStatus.WAITING, "mark as no-show");

        entry.setStatus(QueueStatus.NO_SHOW);
        entry = queueRepository.save(entry);

        log.info("Token #{} marked as NO_SHOW", entry.getQueueNumber());
        return queueMapper.toResponse(entry, null);
    }

    @Override
    @Transactional
    public QueueEntryResponse cancelQueueEntry(UUID queueEntryId) {
        log.info("Cancel requested for queue entry {}", queueEntryId);

        QueueEntry entry = findEntryOrThrow(queueEntryId);
        assertStatus(entry, QueueStatus.WAITING, "cancel");

        entry.setStatus(QueueStatus.CANCELLED);
        entry = queueRepository.save(entry);

        log.info("Token #{} cancelled", entry.getQueueNumber());
        return queueMapper.toResponse(entry, null);
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getTodaysQueue() {
        LocalDate today = LocalDate.now();
        List<QueueEntry> entries = queueRepository.findByQueueDateOrderByQueueNumberAsc(today);

        return entries.stream()
                .map(entry -> queueMapper.toResponse(entry, computePosition(entry)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QueueEntryResponse getCurrentlyServing() {
        LocalDate today = LocalDate.now();
        QueueEntry entry = queueRepository
                .findFirstByQueueDateAndStatus(today, QueueStatus.IN_CONSULTATION)
                .orElseThrow(() -> new QueueEntryNotFoundException(
                        "No patient is currently in consultation"));

        return queueMapper.toResponse(entry, null);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueEntryResponse getNextInQueue() {
        LocalDate today = LocalDate.now();
        List<QueueEntry> waiting = queueRepository
                .findByQueueDateAndStatusOrderByQueueNumberAsc(today, QueueStatus.WAITING);

        if (waiting.isEmpty()) {
            throw new QueueEntryNotFoundException("No patients waiting in the queue");
        }

        QueueEntry next = waiting.get(0);
        return queueMapper.toResponse(next, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientQueueStatusResponse getMyQueueStatus(UUID appointmentId) {
        QueueEntry entry = queueRepository.findByAppointmentAppointmentId(appointmentId)
                .orElseThrow(() -> new QueueEntryNotFoundException(
                        "No queue entry found for appointment " + appointmentId));

        LocalDate today = entry.getQueueDate();

        // Count how many WAITING patients are ahead of this one
        int patientsAhead = queueRepository.countWaitingAhead(today, entry.getQueueNumber());

        // Get the currently-serving token number (if any)
        Integer currentlyServingNumber = queueRepository
                .findFirstByQueueDateAndStatus(today, QueueStatus.IN_CONSULTATION)
                .map(QueueEntry::getQueueNumber)
                .orElse(null);

        return queueMapper.toPatientStatus(entry, patientsAhead, currentlyServingNumber);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private QueueEntry findEntryOrThrow(UUID queueEntryId) {
        return queueRepository.findById(queueEntryId)
                .orElseThrow(() -> new QueueEntryNotFoundException(
                        "Queue entry not found: " + queueEntryId));
    }

    /**
     * Guards state transitions — throws if the entry is not in the expected status.
     */
    private void assertStatus(QueueEntry entry, QueueStatus expected, String action) {
        if (entry.getStatus() != expected) {
            throw new InvalidQueueStateException(
                    "Cannot " + action + " — queue entry " + entry.getQueueEntryId()
                            + " is " + entry.getStatus() + ", expected " + expected);
        }
    }

    /**
     * Computes the 1-based position for WAITING entries.
     * Returns null for non-WAITING entries (position is meaningless once served/cancelled).
     */
    private Integer computePosition(QueueEntry entry) {
        if (entry.getStatus() != QueueStatus.WAITING) {
            return null;
        }
        return queueRepository.countWaitingAhead(entry.getQueueDate(), entry.getQueueNumber()) + 1;
    }
}
