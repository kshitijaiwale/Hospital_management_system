package com.hospital.app.reporting.service;

import com.hospital.app.reporting.dto.TimelineResponse;

import java.util.UUID;

public interface PatientTimelineService {
    TimelineResponse getPatientTimeline(UUID patientId);
}
