package com.hospital.app.patient.repository;

import com.hospital.app.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByUser_UserId(UUID userId);

    @Query("SELECT p FROM Patient p WHERE " +
            "LOWER(p.patientNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Patient> searchPatients(@Param("query") String query);

    @Query("SELECT MAX(CAST(SUBSTRING(p.patientNumber, 10) AS integer)) FROM Patient p WHERE p.patientNumber LIKE 'PAT-2026-%'")
    Integer findMaxPatientNumberSequence();
}
