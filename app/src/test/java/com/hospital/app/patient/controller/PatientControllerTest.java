package com.hospital.app.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.app.patient.dto.request.CreatePatientRequest;
import com.hospital.app.patient.dto.request.UpdatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.enums.BloodGroup;
import com.hospital.app.patient.enums.PatientStatus;
import com.hospital.app.patient.service.PatientRegistrationService;
import com.hospital.app.patient.service.PatientService;
import com.hospital.app.security.util.PatientSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    @MockBean
    private PatientRegistrationService registrationService;

    @MockBean(name = "patientSecurity")
    private PatientSecurity patientSecurity;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/patients (Self-Registration)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/patients — should register patient successfully without authentication")
    void registerPatient_success() throws Exception {
        CreatePatientRequest request = CreatePatientRequest.builder()
                .name("Jane Doe")
                .email("jane@clinic.com")
                .password("secure123")
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .bloodGroup(BloodGroup.A_POS)
                .build();

        PatientResponse response = PatientResponse.builder()
                .patientId(UUID.randomUUID())
                .patientNumber("PAT-2026-000002")
                .name("Jane Doe")
                .email("jane@clinic.com")
                .build();

        when(registrationService.registerPatient(any(CreatePatientRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientNumber").value("PAT-2026-000002"))
                .andExpect(jsonPath("$.name").value("Jane Doe"));

        verify(registrationService).registerPatient(any(CreatePatientRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/patients — should fail registration due to validation error")
    void registerPatient_validationError() throws Exception {
        CreatePatientRequest request = CreatePatientRequest.builder()
                .name("") // invalid
                .email("invalid-email") // invalid
                .password("123") // too short
                .phone("1234567890")
                .dateOfBirth(LocalDate.now().plusDays(1)) // future
                .bloodGroup(BloodGroup.A_POS)
                .build();

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).registerPatient(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/patients/{patientId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/v1/patients/{id} — DOCTOR should be allowed")
    void getPatient_doctorAllowed() throws Exception {
        UUID id = UUID.randomUUID();
        PatientResponse response = PatientResponse.builder().patientId(id).name("Jane Doe").build();

        when(patientService.getPatientById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"));

        verify(patientService).getPatientById(id);
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/patients/{id} — RECEPTIONIST should be allowed")
    void getPatient_receptionistAllowed() throws Exception {
        UUID id = UUID.randomUUID();
        PatientResponse response = PatientResponse.builder().patientId(id).name("Jane Doe").build();

        when(patientService.getPatientById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/" + id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "jane@clinic.com", roles = "PATIENT")
    @DisplayName("GET /api/v1/patients/{id} — PATIENT who is owner should be allowed")
    void getPatient_ownerPatientAllowed() throws Exception {
        UUID id = UUID.randomUUID();
        PatientResponse response = PatientResponse.builder().patientId(id).name("Jane Doe").build();

        when(patientSecurity.isOwner(id)).thenReturn(true);
        when(patientService.getPatientById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/" + id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "other@clinic.com", roles = "PATIENT")
    @DisplayName("GET /api/v1/patients/{id} — PATIENT who is NOT owner should be forbidden")
    void getPatient_nonOwnerPatientForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(patientSecurity.isOwner(id)).thenReturn(false);

        mockMvc.perform(get("/api/v1/patients/" + id))
                .andExpect(status().isForbidden());

        verify(patientService, never()).getPatientById(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/patients/search
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/v1/patients/search — DOCTOR should be allowed")
    void searchPatients_doctorAllowed() throws Exception {
        when(patientService.searchPatients("query")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/patients/search").param("query", "query"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    @DisplayName("GET /api/v1/patients/search — PATIENT should be forbidden")
    void searchPatients_patientForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/patients/search").param("query", "query"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/patients/{patientId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("PUT /api/v1/patients/{id} — RECEPTIONIST should be allowed")
    void updatePatient_receptionistAllowed() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .phone("1112223333")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .bloodGroup(BloodGroup.B_NEG)
                .status(PatientStatus.ACTIVE)
                .build();

        PatientResponse response = PatientResponse.builder().patientId(id).phone("1112223333").build();
        when(patientService.updatePatient(eq(id), any(UpdatePatientRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/patients/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    @DisplayName("PUT /api/v1/patients/{id} — PATIENT who is owner should be allowed")
    void updatePatient_ownerPatientAllowed() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .phone("1112223333")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .bloodGroup(BloodGroup.B_NEG)
                .status(PatientStatus.ACTIVE)
                .build();

        PatientResponse response = PatientResponse.builder().patientId(id).phone("1112223333").build();
        when(patientSecurity.isOwner(id)).thenReturn(true);
        when(patientService.updatePatient(eq(id), any(UpdatePatientRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/patients/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("PUT /api/v1/patients/{id} — DOCTOR should be forbidden")
    void updatePatient_doctorForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .phone("1112223333")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .bloodGroup(BloodGroup.B_NEG)
                .status(PatientStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/patients/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }
}
