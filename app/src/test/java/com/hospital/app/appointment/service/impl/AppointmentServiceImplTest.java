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
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl Unit Tests")
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private AppointmentProperties appointmentProperties;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private UUID patientId;
    private UUID appointmentId;
    private Patient patient;
    private Appointment scheduledAppointment;
    private AppointmentResponse mockResponse;
    private LocalDateTime futureDateTime;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
        futureDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        User user = User.builder()
                .userId(UUID.randomUUID())
                .name("John Doe")
                .email("john@clinic.com")
                .build();

        patient = Patient.builder()
                .patientId(patientId)
                .patientNumber("PAT-2026-000001")
                .user(user)
                .build();

        scheduledAppointment = Appointment.builder()
                .appointmentId(appointmentId)
                .patient(patient)
                .appointmentDateTime(futureDateTime)
                .durationMinutes(30)
                .status(AppointmentStatus.SCHEDULED)
                .notes("Regular check-up")
                .build();

        mockResponse = AppointmentResponse.builder()
                .appointmentId(appointmentId)
                .patientId(patientId)
                .patientName("John Doe")
                .patientNumber("PAT-2026-000001")
                .appointmentDateTime(futureDateTime)
                .durationMinutes(30)
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Book Appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("bookAppointment()")
    class BookAppointmentTests {

        @Test
        @DisplayName("Should book appointment with request-provided duration")
        void bookAppointment_withExplicitDuration() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(45)
                    .notes("Extended visit")
                    .build();

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(appointmentRepository.existsOverlappingAppointment(
                    eq(futureDateTime), eq(futureDateTime.plusMinutes(45)),
                    eq(null), eq(AppointmentStatus.SCHEDULED)))
                    .thenReturn(false);
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(scheduledAppointment);
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(mockResponse);

            AppointmentResponse result = appointmentService.bookAppointment(request);

            assertNotNull(result);
            verify(appointmentRepository).save(any(Appointment.class));

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            assertEquals(45, captor.getValue().getDurationMinutes());
        }

        @Test
        @DisplayName("Should use default duration when not specified in request")
        void bookAppointment_withDefaultDuration() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(null)
                    .build();

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(appointmentProperties.defaultDuration()).thenReturn(30);
            when(appointmentRepository.existsOverlappingAppointment(
                    any(), any(), any(), any()))
                    .thenReturn(false);
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(scheduledAppointment);
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(mockResponse);

            appointmentService.bookAppointment(request);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            assertEquals(30, captor.getValue().getDurationMinutes());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent patient")
        void bookAppointment_patientNotFound() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .build();

            when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> appointmentService.bookAppointment(request));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw SlotNotAvailableException when slot overlaps")
        void bookAppointment_slotConflict() {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(30)
                    .build();

            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(appointmentRepository.existsOverlappingAppointment(
                    eq(futureDateTime), eq(futureDateTime.plusMinutes(30)),
                    eq(null), eq(AppointmentStatus.SCHEDULED)))
                    .thenReturn(true);

            assertThrows(SlotNotAvailableException.class, () -> appointmentService.bookAppointment(request));
            verify(appointmentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reschedule Appointment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rescheduleAppointment()")
    class RescheduleAppointmentTests {

        @Test
        @DisplayName("Should mark original as RESCHEDULED and create new SCHEDULED appointment")
        void reschedule_success() {
            LocalDateTime newDateTime = futureDateTime.plusDays(1);
            RescheduleAppointmentRequest request = RescheduleAppointmentRequest.builder()
                    .newAppointmentDateTime(newDateTime)
                    .notes("Patient requested")
                    .build();

            Appointment newAppointment = Appointment.builder()
                    .appointmentId(UUID.randomUUID())
                    .patient(patient)
                    .appointmentDateTime(newDateTime)
                    .durationMinutes(30)
                    .status(AppointmentStatus.SCHEDULED)
                    .rescheduledFromId(appointmentId)
                    .build();

            AppointmentResponse newResponse = AppointmentResponse.builder()
                    .appointmentId(newAppointment.getAppointmentId())
                    .status(AppointmentStatus.SCHEDULED)
                    .rescheduledFromId(appointmentId)
                    .build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.existsOverlappingAppointment(any(), any(), any(), any())).thenReturn(false);
            when(appointmentRepository.save(any(Appointment.class)))
                    .thenReturn(scheduledAppointment)
                    .thenReturn(newAppointment);
            when(appointmentMapper.toResponse(newAppointment)).thenReturn(newResponse);

            AppointmentResponse result = appointmentService.rescheduleAppointment(appointmentId, request);

            assertNotNull(result);
            assertEquals(appointmentId, result.rescheduledFromId());

            // Verify save was called twice: original update + new creation
            verify(appointmentRepository, times(2)).save(any(Appointment.class));

            // Verify original was set to RESCHEDULED
            assertEquals(AppointmentStatus.RESCHEDULED, scheduledAppointment.getStatus());
        }

        @Test
        @DisplayName("Should reject rescheduling a completed appointment")
        void reschedule_completedAppointment() {
            scheduledAppointment.setStatus(AppointmentStatus.COMPLETED);
            RescheduleAppointmentRequest request = RescheduleAppointmentRequest.builder()
                    .newAppointmentDateTime(futureDateTime.plusDays(1))
                    .build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));

            assertThrows(InvalidAppointmentStateException.class,
                    () -> appointmentService.rescheduleAppointment(appointmentId, request));
            verify(appointmentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State Machine — Cancel / Missed / Complete
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should cancel a SCHEDULED appointment")
        void cancel_success() {
            AppointmentResponse cancelledResponse = AppointmentResponse.builder()
                    .appointmentId(appointmentId).status(AppointmentStatus.CANCELLED).build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.save(any())).thenReturn(scheduledAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(cancelledResponse);

            AppointmentResponse result = appointmentService.cancelAppointment(appointmentId);
            assertEquals(AppointmentStatus.CANCELLED, result.status());
        }

        @Test
        @DisplayName("Should reject cancelling a COMPLETED appointment")
        void cancel_completedFails() {
            scheduledAppointment.setStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));

            assertThrows(InvalidAppointmentStateException.class,
                    () -> appointmentService.cancelAppointment(appointmentId));
        }

        @Test
        @DisplayName("Should reject cancelling a CANCELLED appointment")
        void cancel_alreadyCancelledFails() {
            scheduledAppointment.setStatus(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));

            assertThrows(InvalidAppointmentStateException.class,
                    () -> appointmentService.cancelAppointment(appointmentId));
        }

        @Test
        @DisplayName("Should mark a SCHEDULED appointment as MISSED")
        void markMissed_success() {
            AppointmentResponse missedResponse = AppointmentResponse.builder()
                    .appointmentId(appointmentId).status(AppointmentStatus.MISSED).build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.save(any())).thenReturn(scheduledAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(missedResponse);

            AppointmentResponse result = appointmentService.markMissed(appointmentId);
            assertEquals(AppointmentStatus.MISSED, result.status());
        }

        @Test
        @DisplayName("Should mark a SCHEDULED appointment as COMPLETED")
        void markCompleted_success() {
            AppointmentResponse completedResponse = AppointmentResponse.builder()
                    .appointmentId(appointmentId).status(AppointmentStatus.COMPLETED).build();

            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentRepository.save(any())).thenReturn(scheduledAppointment);
            when(appointmentMapper.toResponse(any())).thenReturn(completedResponse);

            AppointmentResponse result = appointmentService.markCompleted(appointmentId);
            assertEquals(AppointmentStatus.COMPLETED, result.status());
        }

        @Test
        @DisplayName("Should reject marking a MISSED appointment as COMPLETED")
        void markCompleted_missedFails() {
            scheduledAppointment.setStatus(AppointmentStatus.MISSED);
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));

            assertThrows(InvalidAppointmentStateException.class,
                    () -> appointmentService.markCompleted(appointmentId));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read Operations
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Read Operations")
    class ReadOperationTests {

        @Test
        @DisplayName("Should return appointment by ID")
        void getById_success() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(scheduledAppointment));
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(mockResponse);

            AppointmentResponse result = appointmentService.getAppointmentById(appointmentId);
            assertNotNull(result);
            assertEquals(appointmentId, result.appointmentId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown ID")
        void getById_notFound() {
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> appointmentService.getAppointmentById(appointmentId));
        }

        @Test
        @DisplayName("Should return appointments for a patient")
        void getByPatient_success() {
            when(patientRepository.existsById(patientId)).thenReturn(true);
            when(appointmentRepository.findByPatientPatientIdOrderByAppointmentDateTimeDesc(patientId))
                    .thenReturn(List.of(scheduledAppointment));
            when(appointmentMapper.toResponse(scheduledAppointment)).thenReturn(mockResponse);

            List<AppointmentResponse> results = appointmentService.getAppointmentsForPatient(patientId);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent patient")
        void getByPatient_patientNotFound() {
            when(patientRepository.existsById(patientId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> appointmentService.getAppointmentsForPatient(patientId));
        }
    }
}
