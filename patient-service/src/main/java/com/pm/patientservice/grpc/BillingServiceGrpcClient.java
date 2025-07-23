/**
 gRPC client service for communicating with the external Billing service.

 Establishes a connection to the Billing gRPC server using the configured address and port.
 Uses a blocking stub to synchronously call remote billing operations.

 Provides method to create a billing account by sending patient details and receiving a response.
 */
package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    /// Constructor initializes gRPC channel and stub to connect with Billing service.
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort)
    {
        log.info("Connecting to billing service at {}:{}", serverAddress, serverPort);

        /// Create a managed channel to the Billing service
        ManagedChannel channel =  ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext() /// disables TLS for simplicity
                .build();

        /// Create a synchronous/blocking stub for calling the Billing gRPC service
        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        /// Build the gRPC request message
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId).setName(name).setEmail(email)
                .build();

        /// Make the gRPC call and get the response
        BillingResponse response = blockingStub.createBillingAccount(request);

        log.info("Received a response from billing service: {}", response);

        return response;
    }
}
