/**
 * Gateway filter that validates JWT tokens via an external auth service.
 *
 * Extracts the Bearer token from the Authorization header and calls /validate.
 * Rejects requests with HTTP 401 if the token is missing or invalid.
 */

package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JwtValidationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient;
    /**
     * Initializes WebClient with the base URL of the external authentication service.
     *
     */
    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
                                             @Value("${auth.service.url}") String authServiceUrl) {
        /** auth-service:4005
        ecs.aws.askfsdffsd:5000 as example */
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }
    /**
     * Applies the JWT validation logic as a Gateway filter.
     * Extracts the Bearer token and verifies it via the external auth service.
     */
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            /** Reject request if token is missing or not in Bearer format */
            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            }

            /** Validate the token by calling the external auth service.
            If validation succeeds, continue processing the request. */
            return webClient.get()
                    .uri("/validate") /** Call the external auth service's /validate endpoint to check the token. */
                    .header(HttpHeaders.AUTHORIZATION, token) /** Add the Authorization header with the Bearer token from the original request. */
                    .retrieve() /** Retrieve the response but ignore the body, only care about the status code. */
                    .toBodilessEntity() /** If the response indicates success, continue processing the original request by passing it down the filter chain. */
                    .then(chain.filter(exchange));
                    /** After the token validation completes successfully,
                    continue processing the request by passing it to the next filter in the chain. */
        };
    }
}
