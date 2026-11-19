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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for expense-service, resolved through SmallRye Stork ({@code stork://expense-service}).
 * <p>
 * Fault-tolerance policy:
 * <ul>
 *   <li>Every call has a timeout (reads 3s, writes 5s).</li>
 *   <li>GET, PUT and DELETE are idempotent and are retried up to 2 more times, but ONLY when the
 *       downstream call failed at the transport level (connection refused/reset) or timed out.
 *       HTTP error responses (400/401/403/404/409...) are returned as-is and never retried.</li>
 *   <li>POST (create) is never retried - a retry after a timeout could create a duplicate expense.</li>
 * </ul>
 * The caller's already-validated bearer token is passed explicitly; no other client headers are forwarded.
 */
@RegisterRestClient(configKey = "expense-service")
@Path("/api")
public interface ExpenseServiceClient {

    String AUTH = HttpHeaders.AUTHORIZATION;

    // ---- expenses -------------------------------------------------------------------------

    @GET
    @Path("/expenses")
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response listExpenses(@HeaderParam(AUTH) String bearer);

    @GET
    @Path("/expenses/{id}")
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response getExpense(@HeaderParam(AUTH) String bearer, @PathParam("id") long id);

    @POST
    @Path("/expenses")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(5000)
    Response createExpense(@HeaderParam(AUTH) String bearer, String body);

    @PUT
    @Path("/expenses/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response updateExpense(@HeaderParam(AUTH) String bearer, @PathParam("id") long id, String body);

    @DELETE
    @Path("/expenses/{id}")
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response deleteExpense(@HeaderParam(AUTH) String bearer, @PathParam("id") long id);

    // ---- categories -----------------------------------------------------------------------

    @GET
    @Path("/categories")
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response listCategories(@HeaderParam(AUTH) String bearer);

    @GET
    @Path("/categories/{id}")
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response getCategory(@HeaderParam(AUTH) String bearer, @PathParam("id") long id);

    @POST
    @Path("/categories")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(5000)
    Response createCategory(@HeaderParam(AUTH) String bearer, String body);

    @PUT
    @Path("/categories/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response updateCategory(@HeaderParam(AUTH) String bearer, @PathParam("id") long id, String body);

    @DELETE
    @Path("/categories/{id}")
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, retryOn = {ProcessingException.class, TimeoutException.class})
    Response deleteCategory(@HeaderParam(AUTH) String bearer, @PathParam("id") long id);
}
