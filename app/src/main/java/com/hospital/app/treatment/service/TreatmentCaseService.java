package com.hospital.app.treatment.service;

import com.hospital.app.treatment.dto.request.CreateTreatmentCaseRequest;
import com.hospital.app.treatment.dto.response.TreatmentCaseResponse;

import java.util.List;
import java.util.UUID;

public interface TreatmentCaseService {

    TreatmentCaseResponse createTreatmentCase(CreateTreatmentCaseRequest request);

    TreatmentCaseResponse getTreatmentCaseById(UUID caseId);

    List<TreatmentCaseResponse> getTreatmentCasesForPatient(UUID patientId);

    TreatmentCaseResponse closeTreatmentCase(UUID caseId);
}
