package com.org.gateway.error;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Errors that originate in the gateway itself, in the shared {"code", "message", "timestamp"} format.
 * Errors returned by downstream services are relayed unchanged by the routes.
 * Requires quarkus.http.auth.proactive=false so authentication failures reach these mappers.
 */
public class GatewayExceptionMappers {

    private static final Logger LOG = Logger.getLogger(GatewayExceptionMappers.class);

    @ServerExceptionMapper({AuthenticationFailedException.class, UnauthorizedException.class})
    public Response unauthorized() {
        return json(401, "UNAUTHORIZED", "Missing, invalid or expired JWT")
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build();
    }

    @ServerExceptionMapper
    public Response forbidden(ForbiddenException e) {
        return json(403, "FORBIDDEN", "Insufficient role for this resource").build();
    }

    /** Raised (after retries are exhausted) when a downstream service cannot be reached. */
    @ServerExceptionMapper
    public Response downstreamUnavailable(ProcessingException e, UriInfo uri) {
        LOG.warnf("Downstream call for %s failed: %s", uri.getPath(), e.getMessage());
        return json(503, "SERVICE_UNAVAILABLE", "Downstream service is unavailable, please retry later").build();
    }

    @ServerExceptionMapper
    public Response downstreamTimeout(TimeoutException e, UriInfo uri) {
        LOG.warnf("Downstream call for %s timed out", uri.getPath());
        return json(504, "GATEWAY_TIMEOUT", "Downstream service did not respond in time").build();
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uri) {
        int status = e.getResponse().getStatus();
        return switch (status) {
            case 404 -> json(404, "NOT_FOUND", "No route for " + uri.getPath()).build();
            case 405 -> json(405, "METHOD_NOT_ALLOWED", "Method not allowed on " + uri.getPath()).build();
            default -> json(status, "HTTP_" + status, e.getResponse().getStatusInfo().getReasonPhrase()).build();
        };
    }

    private static Response.ResponseBuilder json(int status, String code, String message) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(GatewayError.of(code, message));
    }
}
