package com.hospital.app.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.app.appointment.dto.request.CreateAppointmentRequest;
import com.hospital.app.appointment.dto.request.RescheduleAppointmentRequest;
import com.hospital.app.appointment.dto.response.AppointmentResponse;
import com.hospital.app.appointment.enums.AppointmentStatus;
import com.hospital.app.appointment.service.AppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AppointmentController Integration Tests")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    private final UUID appointmentId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final LocalDateTime futureDateTime = LocalDateTime.now().plusDays(3);

    private AppointmentResponse sampleResponse() {
        return AppointmentResponse.builder()
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
    // POST /api/v1/appointments (Book)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/appointments")
    class BookAppointmentTests {

        @Test
        @WithMockUser(roles = "RECEPTIONIST")
        @DisplayName("RECEPTIONIST should be allowed to book")
        void book_receptionistAllowed() throws Exception {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(30)
                    .build();

            when(appointmentService.bookAppointment(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patientNumber").value("PAT-2026-000001"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN should be allowed to book")
        void book_adminAllowed() throws Exception {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(30)
                    .build();

            when(appointmentService.bookAppointment(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("DOCTOR should be forbidden from booking")
        void book_doctorForbidden() throws Exception {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(30)
                    .build();

            mockMvc.perform(post("/api/v1/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(appointmentService, never()).bookAppointment(any());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("PATIENT should be forbidden from booking")
        void book_patientForbidden() throws Exception {
            CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                    .patientId(patientId)
                    .appointmentDateTime(futureDateTime)
                    .durationMinutes(30)
                    .build();

            mockMvc.perform(post("/api/v1/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/appointments/{id}/reschedule
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/appointments/{id}/reschedule")
    class RescheduleTests {

        @Test
        @WithMockUser(roles = "RECEPTIONIST")
        @DisplayName("RECEPTIONIST should be allowed to reschedule")
        void reschedule_receptionistAllowed() throws Exception {
            RescheduleAppointmentRequest request = RescheduleAppointmentRequest.builder()
                    .newAppointmentDateTime(futureDateTime.plusDays(1))
                    .notes("Patient requested")
                    .build();

            AppointmentResponse response = AppointmentResponse.builder()
                    .appointmentId(UUID.randomUUID())
                    .status(AppointmentStatus.SCHEDULED)
                    .rescheduledFromId(appointmentId)
                    .build();

            when(appointmentService.rescheduleAppointment(eq(appointmentId), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/appointments/" + appointmentId + "/reschedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("DOCTOR should be forbidden from rescheduling")
        void reschedule_doctorForbidden() throws Exception {
            RescheduleAppointmentRequest request = RescheduleAppointmentRequest.builder()
                    .newAppointmentDateTime(futureDateTime.plusDays(1))
                    .build();

            mockMvc.perform(put("/api/v1/appointments/" + appointmentId + "/reschedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/appointments/{id}/complete
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/appointments/{id}/complete")
    class CompleteTests {

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("DOCTOR should be allowed to mark complete")
        void complete_doctorAllowed() throws Exception {
            AppointmentResponse response = AppointmentResponse.builder()
                    .appointmentId(appointmentId)
                    .status(AppointmentStatus.COMPLETED)
                    .build();

            when(appointmentService.markCompleted(appointmentId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/appointments/" + appointmentId + "/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @WithMockUser(roles = "RECEPTIONIST")
        @DisplayName("RECEPTIONIST should be forbidden from marking complete")
        void complete_receptionistForbidden() throws Exception {
            mockMvc.perform(put("/api/v1/appointments/" + appointmentId + "/complete"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/appointments/{id}/cancel & missed
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cancel & Missed Transitions")
    class CancelMissedTests {

        @Test
        @WithMockUser(roles = "RECEPTIONIST")
        @DisplayName("RECEPTIONIST should cancel an appointment")
        void cancel_receptionistAllowed() throws Exception {
            AppointmentResponse response = AppointmentResponse.builder()
                    .appointmentId(appointmentId)
                    .status(AppointmentStatus.CANCELLED)
                    .build();

            when(appointmentService.cancelAppointment(appointmentId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/appointments/" + appointmentId + "/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN should mark missed")
        void missed_adminAllowed() throws Exception {
            AppointmentResponse response = AppointmentResponse.builder()
                    .appointmentId(appointmentId)
                    .status(AppointmentStatus.MISSED)
                    .build();

            when(appointmentService.markMissed(appointmentId)).thenReturn(response);

            mockMvc.perform(put("/api/v1/appointments/" + appointmentId + "/missed"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("MISSED"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET endpoints")
    class ReadEndpointTests {

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("DOCTOR should read appointment by ID")
        void getById_doctorAllowed() throws Exception {
            when(appointmentService.getAppointmentById(appointmentId)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/appointments/" + appointmentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentId").value(appointmentId.toString()));
        }

        @Test
        @WithMockUser(roles = "RECEPTIONIST")
        @DisplayName("RECEPTIONIST should read patient appointments")
        void getByPatient_receptionistAllowed() throws Exception {
            when(appointmentService.getAppointmentsForPatient(patientId)).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/v1/appointments/patient/" + patientId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithMockUser(roles = "DOCTOR")
        @DisplayName("DOCTOR should read today's appointments")
        void getToday_doctorAllowed() throws Exception {
            when(appointmentService.getTodaysAppointments()).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/v1/appointments/today"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        @DisplayName("PATIENT should be forbidden from reading appointments")
        void getToday_patientForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/today"))
                    .andExpect(status().isForbidden());
        }
    }
}
