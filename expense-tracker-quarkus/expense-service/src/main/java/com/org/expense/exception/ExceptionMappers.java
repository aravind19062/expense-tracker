package com.org.expense.exception;

import com.org.expense.dto.ErrorResponse;
import com.org.expense.dto.ErrorResponse.FieldViolation;
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

/**
 * Replaces the Spring {@code @RestControllerAdvice GlobalExceptionHandler}.
 * The original mapped every RuntimeException to 404; here each failure type gets its own status
 * and all errors share one JSON body shape.
 */
public class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class);

    @ServerExceptionMapper
    public Response notFound(ResourceNotFoundException e, UriInfo uri) {
        return build(Response.Status.NOT_FOUND, e.getMessage(), uri);
    }

    @ServerExceptionMapper
    public Response conflict(ConflictException e, UriInfo uri) {
        return build(Response.Status.CONFLICT, e.getMessage(), uri);
    }

    @ServerExceptionMapper
    public Response illegalArgument(IllegalArgumentException e, UriInfo uri) {
        return build(Response.Status.BAD_REQUEST, e.getMessage(), uri);
    }

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e, UriInfo uri) {
        List<FieldViolation> violations = e.getConstraintViolations().stream()
                .map(v -> new FieldViolation(leafField(v), v.getMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        ErrorResponse body = new ErrorResponse(Instant.now(), 400, "Bad Request", "Validation failed",
                uri.getPath(), violations);
        return Response.status(400).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    /** Keeps the status of framework exceptions (e.g. malformed JSON -> 400, unknown path -> 404). */
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
        String path = v.getPropertyPath().toString(); // e.g. "create.request.amount"
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
