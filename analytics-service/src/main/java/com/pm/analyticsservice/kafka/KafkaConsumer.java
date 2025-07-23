/**
 Kafka consumer for handling patient events from the "patient" topic.
 Deserializes Protobuf messages into PatientEvent objects.
 Part of the analytics-service.
 */

package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patients.events.PatientEvent;

@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    /**
     Kafka listener that consumes patient events from the "patient" topic.
     The messages are expected to be in Protobuf binary format.
     */

    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        try {
            /// Deserialize the incoming byte array into a PatientEvent object
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // TODO: Add business logic to process the patient event (e.g., store in DB, trigger workflows)

            log.info("Received Patient Event: [PatientId={}, PatientName={}, PatientEmail={}]",
                    patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());

        } catch (InvalidProtocolBufferException e) {
            /// Log deserialization errors for monitoring and especially debugging
            log.error("Error deserializing event: {}", e.getMessage());
        }
    }
}
