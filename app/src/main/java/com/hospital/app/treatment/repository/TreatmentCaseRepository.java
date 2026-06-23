package com.hospital.app.treatment.repository;

import com.hospital.app.common.enums.CaseStatus;
import com.hospital.app.treatment.entity.TreatmentCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TreatmentCaseRepository extends JpaRepository<TreatmentCase, UUID> {
    List<TreatmentCase> findByPatientPatientIdOrderByOpenDateDesc(UUID patientId);
    long countByStatus(CaseStatus status);
}
