package com.hospital.app.document.repository;

import com.hospital.app.document.entity.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<PatientDocument, UUID> {
    List<PatientDocument> findByPatientPatientIdOrderByUploadedAtDesc(UUID patientId);
}
