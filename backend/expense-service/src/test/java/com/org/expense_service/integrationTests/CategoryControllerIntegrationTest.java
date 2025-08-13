package com.org.expense_service.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.expense_service.model.Category;
import com.org.expense_service.model.Expense;
import com.org.expense_service.repository.CategoryRepository;
import com.org.expense_service.repository.ExpenseRepository;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Rollback(false)
public class CategoryControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private Category category;

    @BeforeEach
    void setup(){
        categoryRepository.deleteAll();
        category = new Category();
        category.setCategoryName("Essentials");
    }

    @Test
    void testCreateCategory() throws Exception{
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Essentials"))
                .andExpect(jsonPath("$.category_id").exists());
    }

    @Test
    void testGetCategoryById() throws Exception{
        Category savedCategory = categoryRepository.save(category);

        mockMvc.perform(get("/api/categories/" + savedCategory.getCategory_id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Essentials"))
                .andExpect(jsonPath("$.category_id").value(savedCategory.getCategory_id()));
    }

    @Test
    void testGetAllCategories() throws Exception{
        Category savedCategory = categoryRepository.save(category);

        Category category1 = new Category();
        category1.setCategoryName("Fun");

        categoryRepository.save(category1);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testUpdateCategory() throws Exception{
        Category savedCategory = categoryRepository.save(category);

        Category updatedCategory = new Category();
        updatedCategory.setCategory_id(savedCategory.getCategory_id());
        updatedCategory.setCategoryName("All Essentials");


        mockMvc.perform(put("/api/categories/"+ savedCategory.getCategory_id(), updatedCategory)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.categoryName").value("All Essentials"))
                .andExpect(jsonPath("$.category_id").value(savedCategory.getCategory_id()));
    }

    @Test
    void testDeleteCategory() throws Exception{
        Category savedCategory = categoryRepository.save(category);

        mockMvc.perform(delete("/api/categories/" + savedCategory.getCategory_id()))
                .andExpect(status().is2xxSuccessful());

        Assertions.assertFalse(categoryRepository.findById(savedCategory.getCategory_id()).isPresent());
    }
}
