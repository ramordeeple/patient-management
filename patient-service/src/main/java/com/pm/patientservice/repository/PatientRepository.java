/**
 Repository interface for performing CRUD operations on the Patient entity.
 Extends JpaRepository to inherit standard methods like save, findById, delete, etc.

 Custom methods:
 - existsByEmail: checks if a patient exists with the given email (for uniqueness validation).
 - existsByEmailAndIdNot: checks if an email belongs to another patient (useful during updates to avoid duplicates).
 */


package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
