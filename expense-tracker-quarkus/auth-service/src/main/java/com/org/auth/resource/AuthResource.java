package com.org.auth.resource;

import com.org.auth.dto.LoginRequest;
import com.org.auth.dto.LoginResponse;
import com.org.auth.dto.RegisterRequest;
import com.org.auth.dto.UserResponse;
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

/** Public endpoints: register, login, and the JWKS that other services use to verify tokens. */
@Path("/api/auth")
@Tag(name = "Auth")
public class AuthResource {

    @Inject
    AuthService authService;
    @Inject
    TokenService tokenService;
    @Inject
    SigningKeyProvider keys;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "201", description = "User registered")
    @APIResponse(responseCode = "400", description = "VALIDATION_FAILED")
    @APIResponse(responseCode = "409", description = "USER_ALREADY_EXISTS")
    public Response register(@Valid @NotNull RegisterRequest request) {
        return Response.status(Response.Status.CREATED).entity(UserResponse.from(authService.register(request))).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "JWT issued")
    @APIResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
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
