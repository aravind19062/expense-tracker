package com.org.gateway.route;

import com.org.gateway.client.AuthServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** Public routes -> auth-service. Same paths as the original Spring Cloud Gateway route {@code /auth/**}. */
@Path("/auth")
@PermitAll
@Tag(name = "Auth (public)")
@Consumes(MediaType.APPLICATION_JSON)
public class AuthRoutes {

    @Inject
    @RestClient
    AuthServiceClient authService;

    @Context
    UriInfo uriInfo;

    @POST
    @Path("/register")
    public Response register(String body) {
        return Relay.of(authService.register(body), uriInfo);
    }

    @POST
    @Path("/login")
    public Response login(String body) {
        return Relay.of(authService.login(body), uriInfo);
    }
}
