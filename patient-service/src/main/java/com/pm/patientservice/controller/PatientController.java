/**
 PatientController handles HTTP requests for managing patient records.

 It exposes RESTful endpoints to:
 - Retrieve all patients (GET /patients)
 - Create a new patient with validation (POST /patients)
 - Update an existing patient by ID (PUT /patients/{id})
 - Delete a patient by ID (DELETE /patients/{id})

 This controller delegates business logic to the PatientService
 and uses DTOs to exchange data between layers.
 */

package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController /// Marks this class as a REST controller (handles HTTP requests/responses)
@RequestMapping("/patients") /// Base URL for all endpoints in this controller
@Tag(name = "Patient", description = "API for managing patients") /// For swagger
public class PatientController {
    private final PatientService patientService;

    /// Constructor injection of the PatientService
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "get patients") /// For swaggery summary
    public ResponseEntity<List<PatientResponseDTO>> getPatients() {
        /// Call the service to get list of all patients
        List<PatientResponseDTO> patients = patientService.getPatients();

        return ResponseEntity.ok().body(patients); /// OK status with the list
    }

    @PostMapping
    @Operation(summary = "create a new patient")
    public ResponseEntity<PatientResponseDTO> createPatient(
            /// Validate request body using default and custom group validators
            @Validated({Default.class, CreatePatientValidationGroup.class})
            @RequestBody PatientRequestDTO patientRequestDTO)
    {
        /// Call service to create a new patient
        PatientResponseDTO patientResponseDTO = patientService.createPatient(patientRequestDTO);

        return ResponseEntity.ok().body(patientResponseDTO); /// OK status with the created patient info
    }

    @PutMapping("/{id}")
    @Operation(summary = "update a patient")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                            @Validated({Default.class})
                                                            @RequestBody PatientRequestDTO patientRequestDTO)
    {
        /// Call service to update existing patient by ID */
        PatientResponseDTO patientResponseDTO = patientService.updatePatient(id, patientRequestDTO);

        return ResponseEntity.ok().body(patientResponseDTO); /// OK status with updated patient info
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete a patient")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        /// Call service to delete patient by ID
        patientService.deletePatient(id);

        return ResponseEntity.noContent().build(); /// Return 204 - successful deletion
    }
}
