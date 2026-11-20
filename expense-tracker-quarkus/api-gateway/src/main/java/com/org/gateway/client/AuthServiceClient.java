package com.org.gateway.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for auth-service ({@code stork://auth-service}).
 * Neither call is retried: registration is not idempotent and a failed login must never be repeated.
 * No Authorization header is ever sent to auth-service.
 */
@RegisterRestClient(configKey = "auth-service")
@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthServiceClient {

    @POST
    @Path("/register")
    @Timeout(5000)
    Response register(String body);

    @POST
    @Path("/login")
    @Timeout(5000)
    Response login(String body);
}
