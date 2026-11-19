package com.org.auth.resource;

import com.org.auth.dto.LoginRequest;
import com.org.auth.dto.LoginResponse;
import com.org.auth.dto.RegisterRequest;
import com.org.auth.service.AuthService;
import com.org.auth.service.SigningKeyProvider;
import com.org.auth.service.TokenService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Migrated from the Spring {@code AuthController}; same paths. All endpoints are public. */
@Path("/auth")
@Tag(name = "Auth")
public class AuthResource {

    @Inject
    AuthService authService;
    @Inject
    TokenService tokenService;
    @Inject
    SigningKeyProvider keys;

    /** Kept as text/plain to match the original contract. */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @APIResponse(responseCode = "201", description = "User registered")
    @APIResponse(responseCode = "400", description = "Validation failed or user already exists")
    public Response register(@Valid @NotNull RegisterRequest request) {
        return Response.status(Response.Status.CREATED).entity(authService.register(request)).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "JWT issued")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public LoginResponse login(@Valid @NotNull LoginRequest request) {
        return new LoginResponse(authService.login(request), tokenService.lifespanSeconds());
    }

    /** Public verification key(s) as a JSON Web Key Set. Consumed by api-gateway and expense-service. */
    @GET
    @Path("/jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public String jwks() {
        return keys.jwksJson();
    }
}
