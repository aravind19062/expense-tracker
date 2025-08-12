package com.org.expense_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.expense_service.model.Category;
import com.org.expense_service.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {
    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;
    @Test
    void testAddCategory() throws Exception {
        Long categoryID = 1L;
        Category category=new Category();
        category.setCategory_id(categoryID);
        category.setCategoryName("newCategory");

        when(categoryService.addCategory(any(Category.class))).thenReturn(category);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category_id").value(categoryID))
                .andExpect(jsonPath("$.categoryName").value("newCategory"));
    }
    @Test
    void testGetAllCategories() throws Exception{
        Category category=new Category();
        category.setCategory_id(1L);
        category.setCategoryName("food");

        Category category1=new Category();
        category1.setCategory_id(2L);
        category1.setCategoryName("rent");

        when(categoryService.getAllCategories()).thenReturn(List.of(category, category1));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category_id").value(1L))
                .andExpect(jsonPath("$[0].categoryName").value("food"))
                .andExpect(jsonPath("$[1].category_id").value(2L))
                .andExpect(jsonPath("$[1].categoryName").value("rent"));
    }
    @Test
    void testGetCategoryById() throws Exception{
        Category category = new Category();
        Long categoryId=3L;
        category.setCategory_id(categoryId);
        category.setCategoryName("savings");
        when(categoryService.getCategoryById(categoryId)).thenReturn(category);
        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category_id").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("savings"));
    }
    @Test
    void testGetCategoryByIdInvalidId() throws Exception{
        Category category=new Category();
        Long categoryId=4L;
        category.setCategory_id(categoryId);
        category.setCategoryName("food");

        when(categoryService.getCategoryById(categoryId)).thenThrow(new RuntimeException("category not found"));
        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("category not found"));
    }
    @Test
    void testUpdateCategoryById() throws Exception{
        Category category=new Category();
        Long categoryId=1L;
        category.setCategory_id(categoryId);
        category.setCategoryName("food");

        Category updatedCategory=new Category();
        updatedCategory.setCategory_id(categoryId);
        updatedCategory.setCategoryName("entertainment");

        when(categoryService.updateCategoryById(eq(categoryId), any(Category.class))).thenReturn(updatedCategory);
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category_id").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("entertainment"));
    }
    @Test
    void testUpdateCategoryByIdInvalidId() throws Exception{
        Long invalidId = 1L;

        Category updatedCategory=new Category();
        updatedCategory.setCategory_id(invalidId);

        when(categoryService.updateCategoryById(eq(invalidId), any(Category.class))).thenThrow(new RuntimeException("id not found"));

        mockMvc.perform(put("/api/categories/{id}", invalidId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("id not found"));
    }
    @Test
    void testDeleteCategoryById() throws Exception{
        Long categoryId = 1L;
        doNothing().when(categoryService).deleteCategoryById(eq(categoryId));

        mockMvc.perform(delete("/api/categories/{id}", categoryId)).andExpect(status().isOk());
    }
    @Test
    void testDeleteCategoryByIdInvalidId() throws Exception {
        Long ivalidId = 1L;

        doThrow(new RuntimeException("id not found")).when(categoryService).deleteCategoryById(ivalidId);

        mockMvc.perform(delete("/api/categories/{id}", ivalidId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("id not found"));
    }
}
