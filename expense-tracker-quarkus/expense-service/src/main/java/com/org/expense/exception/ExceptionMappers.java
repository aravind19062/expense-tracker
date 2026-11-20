package com.org.expense.exception;

import com.org.expense.dto.ErrorResponse;
import com.org.expense.dto.ErrorResponse.FieldError;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Turns every failure into the shared error format {"code", "message", "timestamp"}.
 * Replaces the Spring {@code @RestControllerAdvice GlobalExceptionHandler}, which mapped every
 * RuntimeException to 404.
 */
public class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class);

    @ServerExceptionMapper
    public Response notFound(ResourceNotFoundException e) {
        return error(404, "EXPENSE_NOT_FOUND", e.getMessage());
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

    /** Wrong JSON types or formats, e.g. "expenseDate": "03-11-2026" instead of "2026-11-03". */
    @ServerExceptionMapper
    public Response jsonMismatch(MismatchedInputException e) {
        String field = e.getPath().isEmpty() ? "body" : e.getPath().get(e.getPath().size() - 1).getFieldName();
        String hint = "expenseDate".equals(field) ? " (expected yyyy-MM-dd)" : "";
        return error(400, "MALFORMED_REQUEST", "Invalid value for '" + field + "'" + hint);
    }

    @ServerExceptionMapper({AuthenticationFailedException.class, UnauthorizedException.class})
    public Response unauthorized() {
        return Response.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of("UNAUTHORIZED", "Missing, invalid or expired JWT")).build();
    }

    @ServerExceptionMapper
    public Response forbidden(ForbiddenException e) {
        return error(403, "FORBIDDEN", "Insufficient role for this resource");
    }

    /** Framework errors keep their status: malformed JSON -> 400, unknown path -> 404, wrong method -> 405... */
    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uri) {
        int status = e.getResponse().getStatus();
        return switch (status) {
            case 400 -> error(400, "MALFORMED_REQUEST", "Request body is not valid JSON or has a wrong field type");
            case 404 -> error(404, "NOT_FOUND", "No resource at " + uri.getPath());
            case 405 -> error(405, "METHOD_NOT_ALLOWED", "Method not allowed on " + uri.getPath());
            case 406 -> error(406, "NOT_ACCEPTABLE", "Requested media type is not supported");
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
        String path = v.getPropertyPath().toString(); // e.g. "create.request.amount" or "list.size"
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
