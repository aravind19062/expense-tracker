package com.org.auth.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.org.auth.dto.ErrorResponse;
import com.org.auth.dto.ErrorResponse.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Turns every failure into the shared error format {"code", "message", "timestamp"}. */
public class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class);

    @ServerExceptionMapper
    public Response userExists(UserAlreadyExistsException e) {
        return error(409, "USER_ALREADY_EXISTS", e.getMessage());
    }

    @ServerExceptionMapper
    public Response wrongCredentials(WrongCredException e) {
        return error(401, "INVALID_CREDENTIALS", e.getMessage());
    }

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e) {
        List<FieldError> details = e.getConstraintViolations().stream()
                .map(v -> new FieldError(leafField(v), v.getMessage()))
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList();
        String message = "Validation failed: " + String.join("; ",
                details.stream().map(d -> d.field() + " - " + d.message()).toList());
        return json(400, new ErrorResponse("VALIDATION_FAILED", message, Instant.now(), details));
    }

    @ServerExceptionMapper
    public Response jsonMismatch(MismatchedInputException e) {
        return error(400, "MALFORMED_REQUEST", "Request body has a field of the wrong type");
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uri) {
        int status = e.getResponse().getStatus();
        return switch (status) {
            case 400 -> error(400, "MALFORMED_REQUEST", "Request body is not valid JSON");
            case 404 -> error(404, "NOT_FOUND", "No resource at " + uri.getPath());
            case 405 -> error(405, "METHOD_NOT_ALLOWED", "Method not allowed on " + uri.getPath());
            case 415 -> error(415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json");
            default -> error(status, "HTTP_" + status, e.getResponse().getStatusInfo().getReasonPhrase());
        };
    }

    @ServerExceptionMapper
    public Response unexpected(Exception e, UriInfo uri) {
        LOG.errorf(e, "Unhandled error on %s", uri.getPath());
        return error(500, "INTERNAL_ERROR", "Unexpected server error");
    }

    private static Response error(int status, String code, String message) {
        return json(status, ErrorResponse.of(code, message));
    }

    private static Response json(int status, ErrorResponse body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private static String leafField(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
