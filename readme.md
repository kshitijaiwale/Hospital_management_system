# 🏥 Clinic Management System

A robust, modular, and secure backend system designed for managing hospital and clinic operations, including patient onboarding, appointments, treatments, and billing.

## 🧠 System Definition

The system is designed with a domain-driven approach, separating distinct business concerns into isolated modules while maintaining a unified database and security context.

### Core Modules
1. **Security / Authentication (`com.hospital.app.security`)** — **[COMPLETED]**
   - JWT-based authentication
   - Role-based access control (ADMIN, DOCTOR, RECEPTIONIST, PATIENT)
   - Handles staff registration and secure logins.
2. **Patient Management (`com.hospital.app.patient`)** — **[COMPLETED]**
   - Patient onboarding, profiles, emergency contact details, and audit history.
3. **Appointment Scheduling (`com.hospital.app.appointment`)** — **[COMPLETED]**
   - Booking, rescheduling, and daily schedules.
4. **Treatment & Consultation (`com.hospital.app.treatment`)** — **[COMPLETED]**
   - Treatment cases, doctor consultations, prescriptions, and follow-ups.
5. **Billing & Invoices (`com.hospital.app.billing`)** — *[PENDING]*
   - Invoicing, payment tracking.
6. **Document Management (`com.hospital.app.document`)** — *[PENDING]*
   - Secure medical document uploads and retrievals.

## 🛠 Technology Stack
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Security**: Spring Security 6 with JSON Web Tokens (JJWT)
- **Database**: H2 (In-memory for development/testing) / PostgreSQL (Production ready)
- **ORM**: Hibernate / Spring Data JPA
- **Build Tool**: Maven

## 🚀 Current Progress

### Phase 1: Architecture & Foundation ✅
- [x] Initialized Spring Boot project with all required dependencies.
- [x] Established package structure (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`) for all core modules.
- [x] Implemented `BaseEntity` with `@MappedSuperclass` for automatic audit timestamps (`createdAt`, `updatedAt`).
- [x] Configured H2 database and JPA settings in `application.properties`.

### Phase 2: Security & Authentication Subsystem ✅
- [x] Designed `User` and `Role` entities with a `user_role` many-to-many join table.
- [x] Built `UserRepository` and `RoleRepository` for secure data access.
- [x] Implemented `AuthServiceImpl` (combining `AuthService` and `UserDetailsService`).
- [x] Configured isolated `PasswordEncoderConfig` (BCrypt) to avoid circular dependencies.
- [x] Implemented `RoleSeeder` via `CommandLineRunner` to auto-insert default system roles.
- [x] Developed `JwtUtil` for Bearer token generation and validation.
- [x] Built `JwtAuthenticationFilter` and wired it into `SecurityConfig`.
- [x] Exposed `/api/v1/auth/login` (Permit All) and `/api/v1/users` (ADMIN only).
- [x] Automated Testing: Wrote and passed comprehensive unit and integration tests (16/16 passing) including repository JPA tests, Mockito service tests, and MockMvc security flow tests.

### Phase 3: Patient Module ✅
- [x] Implemented JPA Auditing support (`BaseEntity` + `JpaAuditingConfig`).
- [x] Created `BloodGroup` and `PatientStatus` enums.
- [x] Built the `Patient` domain entity, custom `PatientRepository` and mapper.
- [x] Defined transactional registration/onboarding services with automatic chronological Medical Record Number (MRN) generation.
- [x] Created `PatientSecurity` to evaluate resource ownership in `@PreAuthorize` annotations.
- [x] Implemented `PatientController` protecting search/update/retrieval paths.
- [x] Passed 24 new unit and integration/security tests for the Patient module.

### Phase 4: Appointment Scheduling ✅
- [x] Implemented daily schedule management, booking, rescheduling, and cancellations for patient appointments.

### Phase 5: Treatment & Consultation ✅
- [x] Implemented `TreatmentCase`, `Consultation`, `Prescription`, and `FollowUp` entities with strong relational integrity.
- [x] Enforced robust case management validation (e.g., active vs closed treatment case restrictions).
- [x] Integrated Consultations directly with the Appointment system to seamlessly auto-complete linked visits.
- [x] Developed an optimized, single-transaction bulk creation endpoint for `Prescription` to match clinical workflows perfectly.
- [x] Secured all service layer endpoints utilizing strict `DOCTOR`, `ADMIN`, and `RECEPTIONIST` Role-Based Access Controls.

## 🏃 Getting Started

### Prerequisites
- JDK 17 or higher installed.
- Maven installed.

### Running the Application
To compile and run the application locally:
```bash
# Navigate to the app directory
cd app

# Run Maven tests to verify integrity
./mvnw clean test

# Start the Spring Boot application
./mvnw spring-boot:run
```

By default, the application will run on `http://localhost:8080`.
The in-memory H2 database console can be accessed at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:clinicdb`).
