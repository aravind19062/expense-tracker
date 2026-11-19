package com.org.expense.dto;

import com.org.expense.entity.Expense;

import java.math.BigDecimal;

public record ExpenseResponse(Long id, String title, BigDecimal amount, Long categoryId, String categoryName) {

    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(e.id, e.title, e.amount,
                e.category == null ? null : e.category.id,
                e.category == null ? null : e.category.categoryName);
    }
}
