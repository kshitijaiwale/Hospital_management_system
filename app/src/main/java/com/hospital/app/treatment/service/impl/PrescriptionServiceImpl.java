package com.hospital.app.treatment.service.impl;

import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.treatment.dto.request.AddPrescriptionsRequest;
import com.hospital.app.treatment.dto.request.CreatePrescriptionRequest;
import com.hospital.app.treatment.dto.response.PrescriptionResponse;
import com.hospital.app.treatment.entity.Consultation;
import com.hospital.app.treatment.entity.Prescription;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.mapper.PrescriptionMapper;
import com.hospital.app.treatment.repository.ConsultationRepository;
import com.hospital.app.treatment.repository.PrescriptionRepository;
import com.hospital.app.treatment.service.PrescriptionService;
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
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionMapper prescriptionMapper;

    @Override
    @Transactional
    public List<PrescriptionResponse> addPrescriptions(AddPrescriptionsRequest request) {
        log.info("Adding {} prescriptions to consultation: {}", request.prescriptions().size(), request.consultationId());

        Consultation consultation = consultationRepository.findById(request.consultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + request.consultationId()));

        if (consultation.getTreatmentCase().getStatus() == CaseStatus.CLOSED) {
            throw new InvalidCaseStateException("Cannot add prescriptions to a consultation of a closed treatment case");
        }

        List<Prescription> prescriptionsToSave = request.prescriptions().stream().map(dto ->
                Prescription.builder()
                        .consultation(consultation)
                        .medicationName(dto.medicationName())
                        .dosage(dto.dosage())
                        .frequency(dto.frequency())
                        .duration(dto.duration())
                        .instructions(dto.instructions())
                        .build()
        ).collect(Collectors.toList());

        List<Prescription> savedPrescriptions = prescriptionRepository.saveAll(prescriptionsToSave);
        log.info("Successfully added {} prescriptions", savedPrescriptions.size());

        return savedPrescriptions.stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsForConsultation(UUID consultationId) {
        if (!consultationRepository.existsById(consultationId)) {
            throw new ResourceNotFoundException("Consultation not found with ID: " + consultationId);
        }

        return prescriptionRepository.findByConsultationConsultationId(consultationId)
                .stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }
}
