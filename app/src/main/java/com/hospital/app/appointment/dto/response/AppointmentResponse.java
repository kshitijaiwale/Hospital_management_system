package com.hospital.app.appointment.dto.response;

import com.hospital.app.appointment.enums.AppointmentStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AppointmentResponse(
        UUID appointmentId,
        UUID patientId,
        String patientName,
        String patientNumber,
        LocalDateTime appointmentDateTime,
        int durationMinutes,
        AppointmentStatus status,
        String notes,
        String bookedByUserName,
        UUID rescheduledFromId,
        LocalDateTime createdAt
) {}
