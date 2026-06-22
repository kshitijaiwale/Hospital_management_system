package com.hospital.app.appointment.service;

import com.hospital.app.appointment.dto.request.CreateAppointmentRequest;
import com.hospital.app.appointment.dto.request.RescheduleAppointmentRequest;
import com.hospital.app.appointment.dto.response.AppointmentResponse;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse bookAppointment(CreateAppointmentRequest request);

    AppointmentResponse rescheduleAppointment(UUID appointmentId, RescheduleAppointmentRequest request);

    AppointmentResponse cancelAppointment(UUID appointmentId);

    AppointmentResponse markMissed(UUID appointmentId);

    AppointmentResponse markCompleted(UUID appointmentId);

    AppointmentResponse getAppointmentById(UUID appointmentId);

    List<AppointmentResponse> getAppointmentsForPatient(UUID patientId);

    List<AppointmentResponse> getTodaysAppointments();
}
