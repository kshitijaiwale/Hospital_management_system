package com.hospital.app.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEvent {
    private String eventType; // APPOINTMENT, CONSULTATION, TREATMENT_CASE, DOCUMENT
    private UUID entityId;
    private LocalDateTime eventDate;
    private String title;
    private String description;
    private String status;
    private String performedBy;
}
