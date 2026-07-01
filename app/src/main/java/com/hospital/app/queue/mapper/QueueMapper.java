package com.hospital.app.queue.mapper;

import com.hospital.app.queue.dto.response.PatientQueueStatusResponse;
import com.hospital.app.queue.dto.response.QueueEntryResponse;
import com.hospital.app.queue.entity.QueueEntry;
import com.hospital.app.queue.enums.QueueStatus;
import org.springframework.stereotype.Component;

@Component
public class QueueMapper {

    /**
     * Maps a QueueEntry to the receptionist-facing response.
     * Position must be computed externally and passed in.
     */
    public QueueEntryResponse toResponse(QueueEntry entry, Integer position) {
        var patient = entry.getAppointment().getPatient();
        return QueueEntryResponse.builder()
                .queueEntryId(entry.getQueueEntryId())
                .appointmentId(entry.getAppointment().getAppointmentId())
                .patientId(patient.getPatientId())
                .patientName(patient.getUser().getName())
                .patientNumber(patient.getPatientNumber())
                .queueDate(entry.getQueueDate())
                .queueNumber(entry.getQueueNumber())
                .status(entry.getStatus())
                .position(position)
                .checkInTime(entry.getCheckInTime())
                .consultationStartTime(entry.getConsultationStartTime())
                .consultationEndTime(entry.getConsultationEndTime())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    /**
     * Maps to the lightweight patient-facing status response.
     */
    public PatientQueueStatusResponse toPatientStatus(
            QueueEntry entry,
            int patientsAhead,
            Integer currentlyServingNumber
    ) {
        return PatientQueueStatusResponse.builder()
                .myQueueNumber(entry.getQueueNumber())
                .myStatus(entry.getStatus())
                .patientsAhead(entry.getStatus() == QueueStatus.WAITING ? patientsAhead : null)
                .currentlyServingNumber(currentlyServingNumber)
                .queueDate(entry.getQueueDate())
                .build();
    }
}