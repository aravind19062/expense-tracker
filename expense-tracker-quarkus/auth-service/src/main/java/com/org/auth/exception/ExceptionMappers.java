package com.org.auth.exception;

import com.org.auth.dto.ErrorResponse;
import com.org.auth.dto.ErrorResponse.FieldViolation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Replaces the Spring {@code GlobalExceptionHandler}; status codes are kept (400 duplicate user, 401 bad credentials). */
public class ExceptionMappers {

    @ServerExceptionMapper
    public Response userExists(UserAlreadyExistsException e, UriInfo uri) {
        return build(Response.Status.BAD_REQUEST, e.getMessage(), uri);
    }

    @ServerExceptionMapper
    public Response wrongCredentials(WrongCredException e, UriInfo uri) {
        return build(Response.Status.UNAUTHORIZED, e.getMessage(), uri);
    }

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e, UriInfo uri) {
        List<FieldViolation> violations = e.getConstraintViolations().stream()
                .map(v -> new FieldViolation(leafField(v), v.getMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        return Response.status(400).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(Instant.now(), 400, "Bad Request", "Validation failed", uri.getPath(), violations))
                .build();
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uri) {
        Response.StatusType status = e.getResponse().getStatusInfo();
        String message = status.getStatusCode() == 400 ? "Malformed request body" : status.getReasonPhrase();
        return Response.status(status.getStatusCode()).type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(status.getStatusCode(), status.getReasonPhrase(), message, uri.getPath()))
                .build();
    }

    private static Response build(Response.Status status, String message, UriInfo uri) {
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(status.getStatusCode(), status.getReasonPhrase(), message, uri.getPath()))
                .build();
    }

    private static String leafField(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
