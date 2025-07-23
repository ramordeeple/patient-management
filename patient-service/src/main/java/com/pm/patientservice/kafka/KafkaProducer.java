/**
 KafkaProducer is responsible for sending patient-related events to a Kafka topic.
 Summary:
 - Converts Patient entities into Protobuf-based PatientEvent messages.
 - Serializes the messages into byte arrays.
 - Publishes them to the "patient" Kafka topic.
 - Used when a new patient is created to notify other services (e.g., billing, analytics).
 - Handles errors gracefully with logging for failed sends.
 */


package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patients.events.PatientEvent;

@Service
public class KafkaProducer {
    /// Logger instance for logging events and errors
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    /**
     KafkaTemplate is used to send messages to Kafka
     In this case, the key is a String and the value is a byte array (used for serialized Protobuf message)
     */
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    /// Constructor-based injection of KafkaTemplate
    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     Publishes a PatientCreated event to the "patient" Kafka topic.
     The method:
     1. Builds a PatientEvent message using the given Patient object.
     2. Serializes it to a byte array using Protobuf.
     3. Sends it to the Kafka topic named "patient".
     */
    public void sendEvent(Patient patient) {
        /// Build a Protobuf message (PatientEvent) with the patient details.
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString()) /// sets the patient's UUID as a string

                .setName(patient.getName()) /// sets patient's name

                .setEmail(patient.getEmail()) /// sets patient's email

                .setEventType("PATIENT_CREATED") /// Custom event type label (maybe extended later)

                .build(); /// Finalize the event object

        try {
            /// Send the Protobuf-serialized message (as byte array) to Kafka topic named "patient"
            kafkaTemplate.send("patient", event.toByteArray());
        } catch (Exception e) {
            /// // Log the failure with the event content to help debugging
            log.error("Error sending PatientCreated event: {}", event);
        }
    }
}
