package com.hospital.app.patient.service;

import com.hospital.app.patient.dto.request.UpdatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    PatientResponse getPatientById(UUID patientId);
    List<PatientResponse> searchPatients(String query);
    PatientResponse updatePatient(UUID patientId, UpdatePatientRequest request);
}
