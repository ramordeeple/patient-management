/**
 * gRPC service implementation for billing operations.
 * Handles incoming requests to create billing accounts,
 * logs the request, and responds with a sample account ID and status.
 * (Database integration is a placeholder for future implementation.)
 */


package com.pm.billingservice.grpc;


import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest,
        StreamObserver<billing.BillingResponse> responseObserver) {
        /** Log the incoming create billing account request for tracking */
        log.info("createBillingAccount request received: {}", billingRequest.toString());

        // TODO: Save billingRequest details to the database here

        /** Build a BillingResponse with a fixed account ID and status */
        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId("12345") /** example static account ID */
                .setStatus("ACTIVE") /** example status indicating account is active */
                .build();

        /** Send the BillingResponse back to the client */
        responseObserver.onNext(response);

        /** Signal that response sending is complete */
        responseObserver.onCompleted();
    }
}
