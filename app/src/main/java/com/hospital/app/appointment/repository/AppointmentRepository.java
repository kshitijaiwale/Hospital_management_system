package com.hospital.app.appointment.repository;

import com.hospital.app.appointment.entity.Appointment;
import com.hospital.app.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientPatientIdOrderByAppointmentDateTimeDesc(UUID patientId);
    long countByAppointmentDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Appointment> findByAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
            LocalDateTime start, LocalDateTime end);

    /**
     * True interval overlap detection:
     * Two intervals [requestedStart, requestedEnd) and [existingStart, existingEnd) overlap iff:
     *   requestedStart < existingEnd AND requestedEnd > existingStart
     *
     * Only checks against SCHEDULED appointments (not cancelled/completed/etc).
     * Optionally excludes a specific appointment ID (used during reschedule).
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.status = :activeStatus
              AND (:excludeId IS NULL OR a.appointmentId <> :excludeId)
              AND :requestedStart < timestampadd(MINUTE, a.durationMinutes, a.appointmentDateTime)
              AND :requestedEnd > a.appointmentDateTime
            """)
    boolean existsOverlappingAppointment(
            @Param("requestedStart") LocalDateTime requestedStart,
            @Param("requestedEnd") LocalDateTime requestedEnd,
            @Param("excludeId") UUID excludeId,
            @Param("activeStatus") AppointmentStatus activeStatus);
}
