/**
 Global exception handler for the Patient Service.
 Handles validation errors and custom exceptions in a centralized way
 and returns meaningful error responses to the client.
 */

package com.pm.patientservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /// This method handles errors with messages from @Valid in PatientRequestDTO class
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        /// Collect validation errors by field name and message
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     Handles the case when a user tries to create a patient with an email that already exists.
     Logs the incident and returns a user-friendly message in the response.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex) {
        log.warn("email already exists {}", ex.getMessage() );

        Map<String, String> errors = new HashMap<>();
        errors.put("email", "Email already exists!");
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     Handles the case when a patient with the given ID is not found.
     Logs the event and returns an appropriate error message to the client.
     */
    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePatientNotFoundException(PatientNotFoundException ex) {
        log.warn("patient not found {}", ex.getMessage() );

        Map<String, String> errors = new HashMap<>();
        errors.put("message", "Patient not found!");

        return ResponseEntity.badRequest().body(errors);
    }
}
