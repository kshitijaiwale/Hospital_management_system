package com.hospital.app.treatment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCanAccessTreatmentCases() throws Exception {
        mockMvc.perform(get("/api/v1/treatment-cases/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()); // Expect 404 because it doesn't exist, not 403 Forbidden
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotAccessTreatmentCases() throws Exception {
        mockMvc.perform(get("/api/v1/treatment-cases/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
