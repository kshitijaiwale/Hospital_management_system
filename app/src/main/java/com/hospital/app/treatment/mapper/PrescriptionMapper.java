package com.hospital.app.treatment.mapper;

import com.hospital.app.treatment.dto.response.PrescriptionResponse;
import com.hospital.app.treatment.entity.Prescription;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionMapper {

    public PrescriptionResponse toResponse(Prescription prescription) {
        if (prescription == null) return null;

        return PrescriptionResponse.builder()
                .prescriptionId(prescription.getPrescriptionId())
                .consultationId(prescription.getConsultation().getConsultationId())
                .medicationName(prescription.getMedicationName())
                .dosage(prescription.getDosage())
                .frequency(prescription.getFrequency())
                .duration(prescription.getDuration())
                .instructions(prescription.getInstructions())
                .createdAt(prescription.getCreatedAt())
                .build();
    }
}
