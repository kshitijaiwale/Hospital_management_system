package com.hospital.app.appointment.mapper;

import com.hospital.app.appointment.dto.response.AppointmentResponse;
import com.hospital.app.appointment.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return AppointmentResponse.builder()
                .appointmentId(appointment.getAppointmentId())
                .patientId(appointment.getPatient() != null ? appointment.getPatient().getPatientId() : null)
                .patientName(appointment.getPatient() != null && appointment.getPatient().getUser() != null
                        ? appointment.getPatient().getUser().getName() : null)
                .patientNumber(appointment.getPatient() != null ? appointment.getPatient().getPatientNumber() : null)
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .durationMinutes(appointment.getDurationMinutes())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .bookedByUserName(appointment.getBookedByUser() != null
                        ? appointment.getBookedByUser().getName() : null)
                .rescheduledFromId(appointment.getRescheduledFromId())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}
