package com.hospital.app.appointment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CreateAppointmentRequest(

        @NotNull(message = "Patient ID is required")
        UUID patientId,

        @NotNull(message = "Appointment date and time is required")
        @Future(message = "Appointment must be in the future")
        LocalDateTime appointmentDateTime,

        @Min(value = 15, message = "Duration must be at least 15 minutes")
        Integer durationMinutes,

        String notes
) {}
