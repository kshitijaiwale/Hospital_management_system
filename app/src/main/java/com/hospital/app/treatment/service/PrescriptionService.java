package com.hospital.app.treatment.service;

import com.hospital.app.treatment.dto.request.AddPrescriptionsRequest;
import com.hospital.app.treatment.dto.response.PrescriptionResponse;

import java.util.List;
import java.util.UUID;

public interface PrescriptionService {

    List<PrescriptionResponse> addPrescriptions(AddPrescriptionsRequest request);

    List<PrescriptionResponse> getPrescriptionsForConsultation(UUID consultationId);
}
