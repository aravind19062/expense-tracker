package com.org.expense.resource;

import com.org.expense.dto.CategoryRequest;
import com.org.expense.dto.CategoryResponse;
import com.org.expense.entity.Category;
import com.org.expense.service.CategoryService;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Migrated from the Spring {@code CategoryController}; same paths and operations. */
@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
@Tag(name = "Categories")
@SecurityRequirement(name = "jwt")
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    @GET
    public List<CategoryResponse> getAll() {
        return categoryService.getAll().stream().map(CategoryResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public CategoryResponse getById(@PathParam("id") Long id) {
        return CategoryResponse.from(categoryService.getById(id));
    }

    @POST
    @APIResponse(responseCode = "201", description = "Created")
    public Response create(@Valid @NotNull CategoryRequest request, UriInfo uriInfo) {
        Category created = categoryService.create(request);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build())
                .entity(CategoryResponse.from(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    public CategoryResponse update(@PathParam("id") Long id, @Valid @NotNull CategoryRequest request) {
        return CategoryResponse.from(categoryService.update(id, request));
    }

    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "409", description = "Category still used by expenses")
    public void delete(@PathParam("id") Long id) {
        categoryService.delete(id);
    }
}
