package com.hospital.app.treatment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateConsultationNotesRequest(
        @NotBlank(message = "Clinical notes cannot be empty")
        String clinicalNotes
) {}
