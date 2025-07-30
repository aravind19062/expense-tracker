package com.org.expense_service.service;

import com.org.expense_service.model.Category;
import com.org.expense_service.model.Expense;
import com.org.expense_service.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {
    @Mock
    private ExpenseRepository expenseRepository;
    @InjectMocks
    private ExpenseService expenseService;
    @Test
    void testAddExpense(){
        Category category = new Category();
        category.setCategory_id(1L);
        category.setCategoryName("Food");

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(9.50));
        expense.setCategory(category);
        expense.setTitle("chocolate");

        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        Expense savedExpense = expenseService.addExpense(expense);
        assertNotNull(savedExpense);
        assertEquals("chocolate", savedExpense.getTitle());
        assertEquals(BigDecimal.valueOf(9.50), savedExpense.getAmount());
        assertEquals("Food", savedExpense.getCategory().getCategoryName());
        verify(expenseRepository, times(1)).save(expense);
    }

    @Test
    void testAddExpenseInvalidAmountForNullValue(){
        Expense expense = new Expense();
        expense.setAmount(null);
        expense.setTitle("fudge");
        expense.setCategory(new Category());

        Exception ex = assertThrows(IllegalArgumentException.class, ()->{
            expenseService.addExpense(expense);
        });

        assertEquals("Expense amount must be greater than zero", ex.getMessage());
    }
    @Test
    void testAddExpenseInvalidAmountZero(){
        Expense expense=new Expense();
        expense.setAmount(BigDecimal.ZERO);
        expense.setTitle("fudfge");
        expense.setCategory(new Category());

        Exception ex = assertThrows(IllegalArgumentException.class, ()->{
            expenseService.addExpense(expense);
        });
        assertEquals("Expense amount must be greater than zero", ex.getMessage());
    }
    @Test
    void testUpdateExpenseById(){
        Long expenseId = 1L;
        Category category=new Category();
        category.setCategory_id(1L);
        category.setCategoryName("Food");

        Expense expense=new Expense();
        expense.setId(expenseId);
        expense.setAmount(BigDecimal.valueOf(14.68));
        expense.setTitle("Chocolate");
        expense.setCategory(category);

        Expense updatedExpense = new Expense();
        updatedExpense.setId(expenseId);
        updatedExpense.setAmount(BigDecimal.valueOf(24.68));
        updatedExpense.setTitle("chocolate");
        updatedExpense.setCategory(category);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(updatedExpense);

        Expense result = expenseService.updateExpenseById(expenseId, updatedExpense);
        assertEquals("chocolate", result.getTitle());
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(24.68), result.getAmount());
    }
    @Test
    void testUpdateExpenseIdNotFound(){
        Long expenseId = 1L;

        Category category= new Category();
        category.setCategory_id(1L);
        category.setCategoryName("Food");

        Expense updatedExpense=new Expense();
        updatedExpense.setId(expenseId);
        updatedExpense.setAmount(BigDecimal.valueOf(24.68));
        updatedExpense.setTitle("Chocolate");
        updatedExpense.setCategory(category);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, ()->{
            expenseService.updateExpenseById(expenseId, updatedExpense);
        });
        assertEquals("id not found1", ex.getMessage());

        verify(expenseRepository, times(1)).findById(expenseId);


    }
    @Test
    void testGetAllExpenses(){
        Expense expense=new Expense();
        expense.setCategory(new Category());
        expense.setAmount(BigDecimal.valueOf(45.88));
        expense.setTitle("dosa");

        Expense expense1=new Expense();
        expense1.setCategory(new Category());
        expense1.setTitle("idly");
        expense1.setAmount(BigDecimal.valueOf(34.00));

        List<Expense> list = new ArrayList<>();
        list.add(expense);
        list.add(expense1);

        when(expenseRepository.findAll()).thenReturn(list);
        List<Expense> result = expenseService.getAllExpenses();

        assertNotNull(result);
        assertEquals("dosa", result.get(0).getTitle());
        assertEquals(BigDecimal.valueOf(34.00),result.get(1).getAmount());
        verify(expenseRepository,times(1)).findAll();
    }
    @Test
    void  testGetExpenseById(){
        Long expenseId = 1L;
        Expense expense=new Expense();
        expense.setId(expenseId);
        expense.setCategory(new Category());
        expense.setAmount(BigDecimal.valueOf(10.00));
        expense.setTitle("chocolate");

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        Expense result = expenseService.getExpenseById(expenseId);
        assertNotNull(result);
        assertEquals("chocolate", result.getTitle());
        verify(expenseRepository, times(1)).findById(expenseId);

    }
}
