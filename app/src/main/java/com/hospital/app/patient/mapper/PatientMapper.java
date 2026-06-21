package com.hospital.app.patient.mapper;

import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public PatientResponse toResponse(Patient patient) {
        if (patient == null) {
            return null;
        }

        return PatientResponse.builder()
                .patientId(patient.getPatientId())
                .userId(patient.getUser() != null ? patient.getUser().getUserId() : null)
                .patientNumber(patient.getPatientNumber())
                .name(patient.getUser() != null ? patient.getUser().getName() : null)
                .email(patient.getUser() != null ? patient.getUser().getEmail() : null)
                .phone(patient.getPhone())
                .dateOfBirth(patient.getDateOfBirth())
                .bloodGroup(patient.getBloodGroup())
                .address(patient.getAddress())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .status(patient.getStatus())
                .build();
    }
}
