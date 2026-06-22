package com.hospital.app.treatment.mapper;

import com.hospital.app.treatment.dto.response.ConsultationResponse;
import com.hospital.app.treatment.entity.Consultation;
import org.springframework.stereotype.Component;

@Component
public class ConsultationMapper {

    public ConsultationResponse toResponse(Consultation consultation) {
        if (consultation == null) return null;

        return ConsultationResponse.builder()
                .consultationId(consultation.getConsultationId())
                .treatmentCaseId(consultation.getTreatmentCase().getTreatmentCaseId())
                .appointmentId(consultation.getAppointment() != null ? consultation.getAppointment().getAppointmentId() : null)
                .doctorId(consultation.getDoctor().getUserId())
                .doctorName(consultation.getDoctor().getName())
                .consultationDate(consultation.getConsultationDate())
                .symptoms(consultation.getSymptoms())
                .diagnosis(consultation.getDiagnosis())
                .clinicalNotes(consultation.getClinicalNotes())
                .recommendations(consultation.getRecommendations())
                .build();
    }
}
