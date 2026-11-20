package com.org.gateway.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for expense-service, resolved through SmallRye Stork ({@code stork://expense-service}).
 * <p>
 * Fault tolerance:
 * <ul>
 *   <li>Every call: {@code @Timeout(2000)}.</li>
 *   <li>{@code @Retry(maxRetries = 2)} on the idempotent calls (GET, PUT, DELETE), and only for transport
 *       failures or timeouts. HTTP error responses (400/401/404...) are returned as-is and never retried.</li>
 *   <li>POST (create) is deliberately NOT retried: if the first attempt timed out after the expense was saved,
 *       a retry would create a duplicate.</li>
 * </ul>
 * Only the caller's already-validated bearer token is forwarded; no other client headers.
 */
@RegisterRestClient(configKey = "expense-service")
@Path("/api/expenses")
public interface ExpenseServiceClient {

    String AUTH = HttpHeaders.AUTHORIZATION;

    @GET
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response list(@HeaderParam(AUTH) String bearer, @QueryParam("page") Integer page,
                  @QueryParam("size") Integer size, @QueryParam("category") String category);

    @GET
    @Path("/summary")
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response summary(@HeaderParam(AUTH) String bearer);

    @GET
    @Path("/export/xml")
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response exportXml(@HeaderParam(AUTH) String bearer);

    @GET
    @Path("/{id}")
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response get(@HeaderParam(AUTH) String bearer, @PathParam("id") long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(2000)
    Response create(@HeaderParam(AUTH) String bearer, String body);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response update(@HeaderParam(AUTH) String bearer, @PathParam("id") long id, String body);

    @DELETE
    @Path("/{id}")
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response delete(@HeaderParam(AUTH) String bearer, @PathParam("id") long id);
}
