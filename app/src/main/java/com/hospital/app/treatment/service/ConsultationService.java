package com.hospital.app.treatment.service;

import com.hospital.app.treatment.dto.request.CreateConsultationRequest;
import com.hospital.app.treatment.dto.request.UpdateConsultationNotesRequest;
import com.hospital.app.treatment.dto.response.ConsultationResponse;

import java.util.List;
import java.util.UUID;

public interface ConsultationService {

    ConsultationResponse createConsultation(CreateConsultationRequest request);

    ConsultationResponse addClinicalNotes(UUID consultationId, UpdateConsultationNotesRequest request);

    ConsultationResponse getConsultationById(UUID consultationId);

    List<ConsultationResponse> getConsultationsForCase(UUID treatmentCaseId);
}
