package com.hospital.app.patient.dto.request;

import com.hospital.app.patient.enums.BloodGroup;
import com.hospital.app.patient.enums.PatientStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UpdatePatientRequest(
        @NotBlank(message = "Phone number is required")
        String phone,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "Blood group is required")
        BloodGroup bloodGroup,

        String address,
        String emergencyContactName,
        String emergencyContactPhone,

        @NotNull(message = "Status is required")
        PatientStatus status
) {}
