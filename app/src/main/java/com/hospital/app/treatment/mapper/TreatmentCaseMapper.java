package com.hospital.app.treatment.mapper;

import com.hospital.app.treatment.dto.response.TreatmentCaseResponse;
import com.hospital.app.treatment.entity.TreatmentCase;
import org.springframework.stereotype.Component;

@Component
public class TreatmentCaseMapper {

    public TreatmentCaseResponse toResponse(TreatmentCase treatmentCase) {
        if (treatmentCase == null) return null;

        return TreatmentCaseResponse.builder()
                .treatmentCaseId(treatmentCase.getTreatmentCaseId())
                .patientId(treatmentCase.getPatient().getPatientId())
                .patientName(treatmentCase.getPatient().getUser().getName())
                .title(treatmentCase.getTitle())
                .diagnosis(treatmentCase.getDiagnosis())
                .caseType(treatmentCase.getCaseType())
                .status(treatmentCase.getStatus())
                .openDate(treatmentCase.getOpenDate())
                .closeDate(treatmentCase.getCloseDate())
                .build();
    }
}
