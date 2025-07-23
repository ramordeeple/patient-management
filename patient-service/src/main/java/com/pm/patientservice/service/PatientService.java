/**
 Service class responsible for handling patient-related business logic.
 It interacts with the repository for persistence, sends events to Kafka,
 and makes gRPC calls to the billing service.
 */

package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    /// Repository interface for DB operations on Patient entities
    private final PatientRepository patientRepository;

    /// gRPC client to communicate with the external billing microservice
    private final BillingServiceGrpcClient billingServiceGrpcClient;

    /// Kafka producer to publish domain events (e.g., PatientCreated)
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository,
                          BillingServiceGrpcClient billingServiceGrpcClient,
                          KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    /// Fetches all patients from the database.
    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll(); /// Fetches all patients

        return patients.stream()
                .map(PatientMapper::toDTO) /// Converts each to DTO
                .toList();
    }

    /**
     Creates a new patient:
     1. Validates if the email is unique.
     2. Saves the patient to the DB.
     3. Calls external billing service to create billing account.
     4. Sends a Kafka event to notify the system about the new patient.

     */
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        /// Validates email uniqueness before proceeding
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with this email already exists" +
                    patientRequestDTO.getEmail());
        }

        /// Converts DTO to entity and persist it
        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

        /// Makes gRPC call to external billing service to create a billing account
        billingServiceGrpcClient.createBillingAccount(
                newPatient.getId().toString(),
                newPatient.getName(),
                newPatient.getEmail());

        /// Publish patient creation event to Kafka topic
        kafkaProducer.sendEvent(newPatient);

        /// Returns newly created patient as DTO
        return PatientMapper.toDTO(newPatient);
    }

    /**
     Updates an existing patient record:
     1. Validates patient existence.
     2. Ensures no other patient uses the same email.
     3. Updates fields and saves the changes.
     */
    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        /// Fetches patient by ID or throw exception if not found
        Patient patient = patientRepository
                .findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));


        /// Prevents updating to an email that belongs to another patient
        if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("A patient with this email already exists" +
                    patientRequestDTO.getEmail());
        }

        /** Updates fields with incoming request data
        Note: registeredDate is not updated here to preserve historical accuracy */
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        /// Saves the changes and return updated patient
        return PatientMapper.toDTO(patientRepository.save(patient));
    }

    /**
     Deletes a patient from the database by ID.
     Assumes ID is valid and deletion is idempotent.
     */
    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }
}
