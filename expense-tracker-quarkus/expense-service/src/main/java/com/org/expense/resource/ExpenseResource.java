package com.org.expense.resource;

import com.org.expense.dto.ErrorResponse;
import com.org.expense.dto.ExpenseRequest;
import com.org.expense.dto.ExpenseResponse;
import com.org.expense.entity.Expense;
import com.org.expense.service.ExpenseService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/expenses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
@Tag(name = "Expenses")
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "jwt")
@APIResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
public class ExpenseResource {

    @Inject
    ExpenseService expenseService;

    @Inject
    JsonWebToken jwt;

    @GET
    @APIResponse(responseCode = "200", description = "Expenses owned by the caller")
    public List<ExpenseResponse> getAll() {
        return expenseService.getAll(jwt.getSubject()).stream().map(ExpenseResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "The expense")
    @APIResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ExpenseResponse getById(@PathParam("id") Long id) {
        return ExpenseResponse.from(expenseService.getById(id, jwt.getSubject()));
    }

    @POST
    @APIResponse(responseCode = "201", description = "Created")
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response create(@Valid @NotNull ExpenseRequest request, UriInfo uriInfo) {
        Expense created = expenseService.create(request, jwt.getSubject());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build())
                .entity(ExpenseResponse.from(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "Updated")
    public ExpenseResponse update(@PathParam("id") Long id, @Valid @NotNull ExpenseRequest request) {
        return ExpenseResponse.from(expenseService.update(id, request, jwt.getSubject()));
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Deleted")
    public void delete(@PathParam("id") Long id) {
        expenseService.delete(id, jwt.getSubject());
    }
}
