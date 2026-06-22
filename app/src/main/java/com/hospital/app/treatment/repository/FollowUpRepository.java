package com.hospital.app.treatment.repository;

import com.hospital.app.treatment.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {
    List<FollowUp> findByTreatmentCaseTreatmentCaseIdOrderByFollowUpDateDesc(UUID treatmentCaseId);
}
