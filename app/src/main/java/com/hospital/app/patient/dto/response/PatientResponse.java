package com.hospital.app.patient.dto.response;

import com.hospital.app.patient.enums.BloodGroup;
import com.hospital.app.patient.enums.PatientStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record PatientResponse(
        UUID patientId,
        UUID userId,
        String patientNumber,
        String name,
        String email,
        String phone,
        LocalDate dateOfBirth,
        BloodGroup bloodGroup,
        String address,
        String emergencyContactName,
        String emergencyContactPhone,
        PatientStatus status
) {}
