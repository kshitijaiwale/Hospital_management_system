package com.hospital.app.treatment.mapper;

import com.hospital.app.treatment.dto.response.FollowUpResponse;
import com.hospital.app.treatment.entity.FollowUp;
import org.springframework.stereotype.Component;

@Component
public class FollowUpMapper {

    public FollowUpResponse toResponse(FollowUp followUp) {
        if (followUp == null) return null;

        return FollowUpResponse.builder()
                .followUpId(followUp.getFollowUpId())
                .treatmentCaseId(followUp.getTreatmentCase().getTreatmentCaseId())
                .followUpDate(followUp.getFollowUpDate())
                .reason(followUp.getReason())
                .status(followUp.getStatus())
                .build();
    }
}
