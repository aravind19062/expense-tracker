package com.org.expense_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.expense_service.model.Category;
import com.org.expense_service.model.Expense;
import com.org.expense_service.service.CategoryService;
import com.org.expense_service.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ExpenseControllerTest {
    @MockBean
    private ExpenseService expenseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAddExpense() throws Exception{
        Category category = new Category();
        category.setCategory_id(1L);
        category.setCategoryName("food");

        Expense expense = new Expense();
        expense.setTitle("burgerking");
        expense.setCategory(category);
        expense.setAmount(BigDecimal.valueOf(100));

        when(expenseService.addExpense(any(Expense.class))).thenReturn(expense);

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("burgerking"))
                .andExpect(jsonPath("$.amount").value(BigDecimal.valueOf(100)));
    }

    @Test
    void testAddExpenseInvalidExpense() throws Exception{
        Category category=new Category();

        Expense expense= new Expense();
        expense.setAmount(null);
        expense.setCategory(category);
        expense.setTitle("savings");

        doThrow(new IllegalArgumentException("invalid expense")).when(expenseService).addExpense(any(Expense.class));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Expense amount must be greater than zero"));

    }


    @Test
    void testGetAllExpenses() throws Exception{
        Category category=new Category();
        Category category1=new Category();

        Expense expense=new Expense();
        expense.setTitle("burger");
        expense.setAmount(BigDecimal.valueOf(60));
        expense.setCategory(category);


        Expense expense1=new Expense();
        expense1.setTitle("burger2");
        expense1.setAmount(BigDecimal.valueOf(90));
        expense1.setCategory(category1);

        when(expenseService.getAllExpenses()).thenReturn(List.of(expense, expense1));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("burger"))
                .andExpect(jsonPath("$[1].title").value("burger2"))
                .andExpect(jsonPath("$[0].amount").value(BigDecimal.valueOf(60)))
                .andExpect(jsonPath("$[1].amount").value(BigDecimal.valueOf(90)));
    }
    @Test
    void testGetExpenseById() throws Exception{
        Category category=new Category();

        Long expenseId = 1L;

        Expense expense=new Expense();
        expense.setId(expenseId);
        expense.setAmount(BigDecimal.valueOf(876));
        expense.setCategory(category);
        expense.setTitle("videogames");

        when(expenseService.getExpenseById(eq(expenseId))).thenReturn(expense);

        mockMvc.perform(get("/api/expenses/{id}", expenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("videogames"))
                .andExpect(jsonPath("$.amount").value(BigDecimal.valueOf(876)));
    }
    @Test
    void testGetExpenseByIdInvalidId() throws Exception{
        Long invalidId = 87L;

        when(expenseService.getExpenseById(invalidId)).thenThrow(new RuntimeException("Expense not found"));

        mockMvc.perform(get("/api/expenses/{id}", invalidId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Expense not found"));
    }

}
