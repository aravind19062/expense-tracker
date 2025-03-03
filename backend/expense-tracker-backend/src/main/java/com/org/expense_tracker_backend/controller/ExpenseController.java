package com.org.expense_tracker_backend.controller;

import com.org.expense_tracker_backend.model.Expense;
import com.org.expense_tracker_backend.service.ExpenseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<Expense> getAllExpenses(){
        return expenseService.getALlExpenses();
    }
    @GetMapping("/{id}")
    public Optional<Expense> getExpenseById(@PathVariable Long id){
        return expenseService.getExpenseById(id);
    }

    @PostMapping
    public Expense addExpense(@RequestBody Expense expense){
        return expenseService.addExpense(expense);
    }
}
