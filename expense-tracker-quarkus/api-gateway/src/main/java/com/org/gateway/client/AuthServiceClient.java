package com.org.gateway.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for auth-service. The base URL is {@code stork://auth-service}; SmallRye Stork resolves it
 * to a concrete host:port from the configured static service list.
 * <p>
 * Neither call is retried: both are POSTs (registration is not idempotent) and a 401 from login is
 * an authentication failure, which must never be retried. Only a timeout is applied.
 * No Authorization header is ever sent to auth-service.
 */
@RegisterRestClient(configKey = "auth-service")
@Path("/auth")
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
