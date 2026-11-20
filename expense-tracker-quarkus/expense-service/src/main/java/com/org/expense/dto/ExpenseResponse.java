package com.org.expense.dto;

import com.org.expense.entity.Category;
import com.org.expense.entity.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(Long id, String userId, String title, BigDecimal amount, Category category,
                              LocalDate expenseDate, Instant createdAt) {

    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(e.id, e.userId, e.title, e.amount, e.category, e.expenseDate, e.createdAt);
    }
}
