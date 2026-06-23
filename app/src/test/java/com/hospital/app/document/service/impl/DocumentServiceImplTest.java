package com.hospital.app.document.service.impl;

import com.hospital.app.common.enums.DocumentType;
import com.hospital.app.document.dto.request.UploadDocumentRequest;
import com.hospital.app.document.dto.response.DocumentResponse;
import com.hospital.app.document.entity.PatientDocument;
import com.hospital.app.document.mapper.DocumentMapper;
import com.hospital.app.document.repository.DocumentRepository;
import com.hospital.app.document.service.FileStorageService;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private DocumentMapper documentMapper;

    @InjectMocks private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@test.com", "pass", Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadDocument_success() {
        UUID patientId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test".getBytes());
        
        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .patientId(patientId)
                .documentType(DocumentType.LAB_REPORT)
                .file(file)
                .build();

        Patient patient = new Patient();
        patient.setPatientId(patientId);
        
        User user = new User();
        user.setEmail("test@test.com");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(fileStorageService.storeFile(file)).thenReturn("uuid-test.pdf");
        
        PatientDocument doc = new PatientDocument();
        doc.setDocumentId(UUID.randomUUID());
        
        when(documentRepository.save(any())).thenReturn(doc);
        when(documentMapper.toResponse(any())).thenReturn(new DocumentResponse());

        DocumentResponse response = documentService.uploadDocument(request);
        
        assertNotNull(response);
        verify(documentRepository).save(any());
    }
}
