package com.hospital.app.treatment.service.impl;

import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.common.enums.FollowUpStatus;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.treatment.dto.request.CreateFollowUpRequest;
import com.hospital.app.treatment.dto.request.UpdateFollowUpRequest;
import com.hospital.app.treatment.dto.response.FollowUpResponse;
import com.hospital.app.treatment.entity.FollowUp;
import com.hospital.app.treatment.entity.TreatmentCase;
import com.hospital.app.treatment.exception.InvalidCaseStateException;
import com.hospital.app.treatment.exception.TreatmentNotFoundException;
import com.hospital.app.treatment.mapper.FollowUpMapper;
import com.hospital.app.treatment.repository.FollowUpRepository;
import com.hospital.app.treatment.repository.TreatmentCaseRepository;
import com.hospital.app.treatment.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpServiceImpl implements FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final TreatmentCaseRepository treatmentCaseRepository;
    private final FollowUpMapper followUpMapper;

    @Override
    @Transactional
    public FollowUpResponse scheduleFollowUp(CreateFollowUpRequest request) {
        log.info("Scheduling follow-up for case: {}", request.treatmentCaseId());

        TreatmentCase treatmentCase = treatmentCaseRepository.findById(request.treatmentCaseId())
                .orElseThrow(() -> new TreatmentNotFoundException("Treatment Case not found with ID: " + request.treatmentCaseId()));

        if (treatmentCase.getStatus() == CaseStatus.CLOSED) {
            throw new InvalidCaseStateException("Cannot schedule a follow-up for a closed treatment case");
        }

        if (!request.followUpDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Follow-up date must be in the future");
        }

        FollowUp followUp = FollowUp.builder()
                .treatmentCase(treatmentCase)
                .followUpDate(request.followUpDate())
                .reason(request.reason())
                .status(FollowUpStatus.PENDING)
                .build();

        FollowUp saved = followUpRepository.save(followUp);
        log.info("Scheduled follow-up with ID: {}", saved.getFollowUpId());

        return followUpMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FollowUpResponse updateFollowUpStatus(UUID followUpId, UpdateFollowUpRequest request) {
        log.info("Updating status of follow-up {} to {}", followUpId, request.status());

        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found with ID: " + followUpId));

        followUp.setStatus(request.status());
        FollowUp saved = followUpRepository.save(followUp);

        return followUpMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpResponse> getFollowUpsForCase(UUID treatmentCaseId) {
        if (!treatmentCaseRepository.existsById(treatmentCaseId)) {
            throw new TreatmentNotFoundException("Treatment Case not found with ID: " + treatmentCaseId);
        }

        return followUpRepository.findByTreatmentCaseTreatmentCaseIdOrderByFollowUpDateDesc(treatmentCaseId)
                .stream()
                .map(followUpMapper::toResponse)
                .collect(Collectors.toList());
    }
}
