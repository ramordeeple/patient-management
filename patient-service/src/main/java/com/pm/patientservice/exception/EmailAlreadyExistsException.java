/**
 Custom exception thrown when attempting to create or update a patient
 with an email that already exists in the system.
 */


package com.pm.patientservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
