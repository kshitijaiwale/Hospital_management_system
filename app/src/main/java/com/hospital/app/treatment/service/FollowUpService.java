package com.hospital.app.treatment.service;

import com.hospital.app.treatment.dto.request.CreateFollowUpRequest;
import com.hospital.app.treatment.dto.request.UpdateFollowUpRequest;
import com.hospital.app.treatment.dto.response.FollowUpResponse;

import java.util.List;
import java.util.UUID;

public interface FollowUpService {

    FollowUpResponse scheduleFollowUp(CreateFollowUpRequest request);

    FollowUpResponse updateFollowUpStatus(UUID followUpId, UpdateFollowUpRequest request);

    List<FollowUpResponse> getFollowUpsForCase(UUID treatmentCaseId);
}
