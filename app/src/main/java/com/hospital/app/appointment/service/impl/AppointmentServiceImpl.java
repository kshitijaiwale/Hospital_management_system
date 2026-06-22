package com.hospital.app.appointment.service.impl;

import com.hospital.app.appointment.config.AppointmentProperties;
import com.hospital.app.appointment.dto.request.CreateAppointmentRequest;
import com.hospital.app.appointment.dto.request.RescheduleAppointmentRequest;
import com.hospital.app.appointment.dto.response.AppointmentResponse;
import com.hospital.app.appointment.entity.Appointment;
import com.hospital.app.appointment.enums.AppointmentStatus;
import com.hospital.app.appointment.exception.InvalidAppointmentStateException;
import com.hospital.app.appointment.exception.SlotNotAvailableException;
import com.hospital.app.appointment.mapper.AppointmentMapper;
import com.hospital.app.appointment.repository.AppointmentRepository;
import com.hospital.app.appointment.service.AppointmentService;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final Set<AppointmentStatus> TERMINAL_STATUSES = Set.of(
            AppointmentStatus.COMPLETED,
            AppointmentStatus.CANCELLED,
            AppointmentStatus.MISSED,
            AppointmentStatus.RESCHEDULED
    );

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentProperties appointmentProperties;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(CreateAppointmentRequest request) {
        log.info("Booking appointment for patient ID: {} at {}", request.patientId(), request.appointmentDateTime());

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.patientId()));

        int duration = request.durationMinutes() != null
                ? request.durationMinutes()
                : appointmentProperties.defaultDuration();

        validateSlotAvailability(request.appointmentDateTime(), duration, null);

        User bookedBy = resolveCurrentUser();

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .bookedByUser(bookedBy)
                .appointmentDateTime(request.appointmentDateTime())
                .durationMinutes(duration)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment booked successfully with ID: {}", saved.getAppointmentId());

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(UUID appointmentId, RescheduleAppointmentRequest request) {
        log.info("Rescheduling appointment ID: {} to {}", appointmentId, request.newAppointmentDateTime());

        Appointment original = findAppointmentOrThrow(appointmentId);
        validateMutable(original);

        validateSlotAvailability(request.newAppointmentDateTime(), original.getDurationMinutes(), null);

        // Mark the original as RESCHEDULED
        original.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(original);

        // Create a new SCHEDULED appointment linked to the original
        Appointment newAppointment = Appointment.builder()
                .patient(original.getPatient())
                .bookedByUser(resolveCurrentUser())
                .appointmentDateTime(request.newAppointmentDateTime())
                .durationMinutes(original.getDurationMinutes())
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes() != null ? request.notes() : original.getNotes())
                .rescheduledFromId(original.getAppointmentId())
                .build();

        Appointment saved = appointmentRepository.save(newAppointment);
        log.info("Appointment rescheduled: old={} (RESCHEDULED), new={} (SCHEDULED)",
                original.getAppointmentId(), saved.getAppointmentId());

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(UUID appointmentId) {
        log.info("Cancelling appointment ID: {}", appointmentId);

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateMutable(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment cancelled successfully: {}", appointmentId);

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse markMissed(UUID appointmentId) {
        log.info("Marking appointment as missed: {}", appointmentId);

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateMutable(appointment);

        appointment.setStatus(AppointmentStatus.MISSED);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment marked as missed: {}", appointmentId);

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse markCompleted(UUID appointmentId) {
        log.info("Marking appointment as completed: {}", appointmentId);

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateMutable(appointment);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment marked as completed: {}", appointmentId);

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID appointmentId) {
        log.info("Fetching appointment by ID: {}", appointmentId);
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsForPatient(UUID patientId) {
        log.info("Fetching appointments for patient ID: {}", patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
        }

        return appointmentRepository.findByPatientPatientIdOrderByAppointmentDateTimeDesc(patientId)
                .stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodaysAppointments() {
        log.info("Fetching today's appointments");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return appointmentRepository.findByAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(startOfDay, endOfDay)
                .stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State Machine Guard
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Only SCHEDULED appointments may be mutated.
     * Any terminal status (COMPLETED, CANCELLED, MISSED, RESCHEDULED) blocks further transitions.
     */
    private void validateMutable(Appointment appointment) {
        if (TERMINAL_STATUSES.contains(appointment.getStatus())) {
            throw new InvalidAppointmentStateException(
                    String.format("Cannot modify appointment %s — current status is %s",
                            appointment.getAppointmentId(), appointment.getStatus()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // True Interval Overlap Detection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the requested [start, start+duration) interval does not overlap
     * with any existing SCHEDULED appointment.
     */
    private void validateSlotAvailability(LocalDateTime requestedStart, int durationMinutes, UUID excludeId) {
        LocalDateTime requestedEnd = requestedStart.plusMinutes(durationMinutes);

        boolean overlaps = appointmentRepository.existsOverlappingAppointment(
                requestedStart, requestedEnd, excludeId, AppointmentStatus.SCHEDULED);

        if (overlaps) {
            throw new SlotNotAvailableException(
                    String.format("Time slot %s to %s is not available — overlaps with an existing appointment",
                            requestedStart, requestedEnd));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Appointment findAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }
}
