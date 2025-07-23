/**
 Custom runtime exception indicating that a requested patient was not found in the system.
 */
package com.pm.patientservice.exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String message) {
        super(message);
    }
}
