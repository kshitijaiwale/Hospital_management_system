package com.hospital.app.patient.service.impl;

import com.hospital.app.exception.EmailAlreadyExistsException;
import com.hospital.app.exception.RoleNotFoundException;
import com.hospital.app.patient.dto.request.CreatePatientRequest;
import com.hospital.app.patient.dto.response.PatientResponse;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.mapper.PatientMapper;
import com.hospital.app.patient.repository.PatientRepository;
import com.hospital.app.patient.service.PatientRegistrationService;
import com.hospital.app.security.entity.Role;
import com.hospital.app.security.entity.RoleType;
import com.hospital.app.security.entity.User;
import com.hospital.app.security.repository.RoleRepository;
import com.hospital.app.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientRegistrationServiceImpl implements PatientRegistrationService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponse registerPatient(CreatePatientRequest request) {
        log.info("Starting patient registration for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed - email already exists: {}", request.email());
            throw new EmailAlreadyExistsException("Email is already registered.");
        }

        Role patientRole = roleRepository.findByName(RoleType.PATIENT)
                .orElseThrow(() -> new RoleNotFoundException("PATIENT role not found in the system"));

        User newUser = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isEnabled(true)
                .roles(Set.of(patientRole))
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("User identity created successfully with ID: {}", savedUser.getUserId());

        String newMrn = generateNextPatientNumber();

        Patient newPatient = Patient.builder()
                .patientNumber(newMrn)
                .user(savedUser)
                .phone(request.phone())
                .dateOfBirth(request.dateOfBirth())
                .bloodGroup(request.bloodGroup())
                .address(request.address())
                .emergencyContactName(request.emergencyContactName())
                .emergencyContactPhone(request.emergencyContactPhone())
                .build();

        Patient savedPatient = patientRepository.save(newPatient);
        log.info("Patient profile created successfully with MRN: {}", savedPatient.getPatientNumber());

        return patientMapper.toResponse(savedPatient);
    }

    private String generateNextPatientNumber() {
        Integer maxSeq = patientRepository.findMaxPatientNumberSequence();
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;
        return String.format("PAT-2026-%06d", nextSeq);
    }
}
