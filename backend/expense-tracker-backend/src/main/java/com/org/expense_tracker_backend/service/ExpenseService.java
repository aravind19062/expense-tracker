package com.org.expense_tracker_backend.service;

import com.org.expense_tracker_backend.model.Expense;
import com.org.expense_tracker_backend.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> getALlExpenses(){
        return expenseRepository.findAll();
    }

    public Optional<Expense> getExpenseById(Long id){
        return expenseRepository.findById(id);
    }

    public Expense addExpense(Expense expense){
        return expenseRepository.save(expense);
    }
}
