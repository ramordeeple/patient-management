/**
 Global exception handler for handling JWT-related authorization errors
 returned from downstream services via WebClient.

 Sets HTTP 401 Unauthorized status when WebClient receives a 401 response.
 */

package com.pm.apigateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 Global exception handler for handling unauthorized responses from WebClient calls.
 */

@RestControllerAdvice
public class JwtValidationException {
    /**
     Handles 401 Unauthorized exceptions thrown by WebClient
     and sets the HTTP response status to 401.
     */
    @ExceptionHandler(WebClientResponseException.Unauthorized.class)
    public Mono<Void> handleUnauthorizedException(ServerWebExchange exchange) {
        // Set the HTTP status code to 401 Unauthorized
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // Complete the response without a body
        return exchange.getResponse().setComplete();
    }

}
