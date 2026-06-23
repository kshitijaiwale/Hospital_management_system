package com.hospital.app.treatment.repository;

import com.hospital.app.treatment.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {
    List<Consultation> findByTreatmentCaseTreatmentCaseIdOrderByConsultationDateDesc(UUID treatmentCaseId);
    List<Consultation> findByTreatmentCasePatientPatientIdOrderByConsultationDateDesc(UUID patientId);
    long countByConsultationDateBetween(LocalDateTime start, LocalDateTime end);
}
