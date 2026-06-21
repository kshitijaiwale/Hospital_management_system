package com.hospital.app.patient.service;

import com.hospital.app.patient.dto.request.CreatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;

public interface PatientRegistrationService {
    PatientResponse registerPatient(CreatePatientRequest request);
}
