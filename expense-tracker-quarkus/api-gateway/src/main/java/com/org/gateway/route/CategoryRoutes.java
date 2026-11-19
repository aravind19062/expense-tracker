package com.org.gateway.route;

import com.org.gateway.client.ExpenseServiceClient;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** Protected routes -> expense-service ({@code /api/categories/**}), as in the original gateway. */
@Path("/api/categories")
@RolesAllowed("USER")
@Tag(name = "Categories")
@SecurityRequirement(name = "jwt")
public class CategoryRoutes {

    @Inject
    @RestClient
    ExpenseServiceClient expenseService;

    @Inject
    JsonWebToken jwt;

    @Context
    UriInfo uriInfo;

    private String bearer() {
        return "Bearer " + jwt.getRawToken();
    }

    @GET
    public Response list() {
        return Relay.of(expenseService.listCategories(bearer()), uriInfo);
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        return Relay.of(expenseService.getCategory(bearer(), id), uriInfo);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(String body) {
        return Relay.of(expenseService.createCategory(bearer(), body), uriInfo);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") long id, String body) {
        return Relay.of(expenseService.updateCategory(bearer(), id, body), uriInfo);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        return Relay.of(expenseService.deleteCategory(bearer(), id), uriInfo);
    }
}
