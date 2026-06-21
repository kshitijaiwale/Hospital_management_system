package com.hospital.app.patient.service.impl;

import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.dto.request.UpdatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.mapper.PatientMapper;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID patientId) {
        log.info("Fetching patient profile for ID: {}", patientId);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> searchPatients(String query) {
        log.info("Searching patients with query: {}", query);
        return patientRepository.searchPatients(query).stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(UUID patientId, UpdatePatientRequest request) {
        log.info("Updating patient profile for ID: {}", patientId);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        patient.setPhone(request.phone());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setBloodGroup(request.bloodGroup());
        patient.setAddress(request.address());
        patient.setEmergencyContactName(request.emergencyContactName());
        patient.setEmergencyContactPhone(request.emergencyContactPhone());
        patient.setStatus(request.status());

        Patient updatedPatient = patientRepository.save(patient);
        log.info("Patient profile updated successfully for ID: {}", patientId);
        return patientMapper.toResponse(updatedPatient);
    }
}
