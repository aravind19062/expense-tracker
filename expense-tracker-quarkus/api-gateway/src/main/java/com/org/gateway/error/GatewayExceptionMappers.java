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
 * Consistent JSON errors for failures that originate in the gateway itself.
 * Errors returned by downstream services (400/404/409...) are relayed unchanged by the routes.
 * Requires quarkus.http.auth.proactive=false so authentication failures reach these mappers.
 */
public class GatewayExceptionMappers {

    private static final Logger LOG = Logger.getLogger(GatewayExceptionMappers.class);

    @ServerExceptionMapper({AuthenticationFailedException.class, UnauthorizedException.class})
    public Response unauthorized(UriInfo uri) {
        return json(401, "Unauthorized", "Missing, invalid or expired JWT", uri)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build();
    }

    @ServerExceptionMapper
    public Response forbidden(ForbiddenException e, UriInfo uri) {
        return json(403, "Forbidden", "Insufficient role for this resource", uri).build();
    }

    /** Raised (after retries are exhausted) when a downstream service cannot be reached. */
    @ServerExceptionMapper
    public Response downstreamUnavailable(ProcessingException e, UriInfo uri) {
        LOG.warnf("Downstream call for %s failed: %s", uri.getPath(), e.getMessage());
        return json(503, "Service Unavailable", "Downstream service is unavailable, please retry later", uri).build();
    }

    @ServerExceptionMapper
    public Response downstreamTimeout(TimeoutException e, UriInfo uri) {
        LOG.warnf("Downstream call for %s timed out", uri.getPath());
        return json(504, "Gateway Timeout", "Downstream service did not respond in time", uri).build();
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e, UriInfo uri) {
        Response.StatusType status = e.getResponse().getStatusInfo();
        return json(status.getStatusCode(), status.getReasonPhrase(), status.getReasonPhrase(), uri).build();
    }

    private static Response.ResponseBuilder json(int status, String error, String message, UriInfo uri) {
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(GatewayError.of(status, error, message, uri.getPath()));
    }
}
