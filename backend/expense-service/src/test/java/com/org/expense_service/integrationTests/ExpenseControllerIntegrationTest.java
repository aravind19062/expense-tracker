package com.org.expense_service.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.expense_service.model.Category;
import com.org.expense_service.model.Expense;
import com.org.expense_service.repository.CategoryRepository;
import com.org.expense_service.repository.ExpenseRepository;
import org.apache.commons.configuration.AbstractFileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Rollback(false)
public class ExpenseControllerIntegrationTest {
    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Expense expense;
    private Category category;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp(){
        expenseRepository.deleteAll();

        category=new Category();
        category.setCategoryName("All Essentials");
        Category savedCategory = categoryRepository.save(category);


        expense = new Expense();
        expense.setCategory(savedCategory);
        expense.setAmount(BigDecimal.valueOf(200));
        expense.setTitle("groceries");
    }

    @Test
    void testCreateExpense() throws Exception{
        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("groceries"))
                .andExpect(jsonPath("$.amount").value(BigDecimal.valueOf(200)))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testGetExpenseById() throws Exception{
        String response = mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                        .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Expense createdExpense = objectMapper.readValue(response, Expense.class);

        mockMvc.perform(get("/api/expenses/" + createdExpense.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdExpense.getId()))
                .andExpect(jsonPath("$.title").value("groceries"));


    }

    @Test
    void testGetAllExpenses() throws Exception{
        Expense savedExpense = expenseRepository.save(expense);

        Expense expense1 = new Expense();
        expense1.setAmount(BigDecimal.valueOf(456));
        expense1.setCategory(category);
        expense1.setTitle("vegetables");
        expenseRepository.save(expense1);

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].amount").value(BigDecimal.valueOf(456.0)))
                .andExpect(jsonPath("$[0].amount").value(BigDecimal.valueOf(200.0)));
    }





}
