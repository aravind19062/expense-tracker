package com.org.expense.resource;

import com.org.expense.dto.ErrorResponse;
import com.org.expense.dto.ExpenseRequest;
import com.org.expense.dto.ExpenseResponse;
import com.org.expense.entity.Category;
import com.org.expense.entity.Expense;
import com.org.expense.mapper.ExpenseMapper;
import com.org.expense.service.ExpenseService;
import com.org.expense.xml.LegacyExpenseExport;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Path("/api/expenses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
@Tag(name = "Expenses")
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "jwt")
@APIResponse(responseCode = "401", description = "Missing, invalid or expired JWT",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class ExpenseResource {

    @Inject
    ExpenseService expenseService;

    @Inject
    ExpenseMapper expenseMapper;

    @Inject
    JsonWebToken jwt;

    private String userId() {
        return jwt.getSubject();
    }

    @POST
    @Operation(summary = "Create an expense")
    @APIResponse(responseCode = "201", description = "Created expense; Location points at it")
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response create(@Valid @NotNull ExpenseRequest request, UriInfo uriInfo) {
        Expense created = expenseService.create(request, userId());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build())
                .entity(ExpenseResponse.from(created))
                .build();
    }

    @GET
    @Operation(summary = "List the caller's expenses, newest first, one page at a time")
    @APIResponse(responseCode = "200", description = "One page of expenses",
            headers = @Header(name = "X-Total-Count", description = "Number of matching expenses across all pages"))
    @APIResponse(responseCode = "400", description = "Invalid page, size or category",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response list(
            @QueryParam("page") @DefaultValue("0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @QueryParam("size") @DefaultValue("10") @Min(value = 1, message = "size must be between 1 and 100")
            @Max(value = 100, message = "size must be between 1 and 100") int size,
            @QueryParam("category") @Pattern(regexp = Category.PATTERN,
                    message = "category must be one of FOOD, TRAVEL, BILLS, SHOPPING, OTHER") String category) {
        ExpenseService.PageResult result = expenseService.list(userId(),
                category == null ? null : Category.valueOf(category), page, size);
        List<ExpenseResponse> body = result.items().stream().map(ExpenseResponse::from).toList();
        return Response.ok(body).header("X-Total-Count", result.totalCount()).build();
    }

    @GET
    @Path("/summary")
    @Operation(summary = "Total spent per category, e.g. {\"FOOD\": 120.50, \"TRAVEL\": 0.00, ...}")
    @APIResponse(responseCode = "200", description = "Totals for every category")
    public Map<Category, BigDecimal> summary() {
        return expenseService.summary(userId());
    }

    @GET
    @Path("/export/xml")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(summary = "All of the caller's expenses in the legacy accounting XML format")
    @APIResponse(responseCode = "200", description = "<Expenses><Expense><Description/><Amt/><CatCode/><TxnDate/></Expense></Expenses>")
    public LegacyExpenseExport exportXml() {
        return expenseMapper.toLegacyExport(
                expenseService.listAll(userId()).stream().map(ExpenseResponse::from).toList());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get one expense")
    @APIResponse(responseCode = "200", description = "The expense")
    @APIResponse(responseCode = "404", description = "EXPENSE_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ExpenseResponse getById(@PathParam("id") Long id) {
        return ExpenseResponse.from(expenseService.getById(id, userId()));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Replace an expense (full body)")
    @APIResponse(responseCode = "200", description = "Updated expense")
    @APIResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "EXPENSE_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ExpenseResponse update(@PathParam("id") Long id, @Valid @NotNull ExpenseRequest request) {
        return ExpenseResponse.from(expenseService.update(id, request, userId()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an expense")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "EXPENSE_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(@PathParam("id") Long id) {
        expenseService.delete(id, userId());
    }
}
