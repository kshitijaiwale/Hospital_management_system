package com.hospital.app.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.appointment")
public record AppointmentProperties(
        int defaultDuration
) {}
