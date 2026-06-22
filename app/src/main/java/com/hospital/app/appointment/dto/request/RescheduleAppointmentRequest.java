package com.hospital.app.appointment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RescheduleAppointmentRequest(

        @NotNull(message = "New appointment date and time is required")
        @Future(message = "New appointment must be in the future")
        LocalDateTime newAppointmentDateTime,

        String notes
) {}
