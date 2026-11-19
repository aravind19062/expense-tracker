package com.org.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for creating/updating an expense.
 * The original API accepted the entity directly; the "amount must be greater than zero"
 * rule from the Spring controller/service is now a declarative constraint.
 */
public record ExpenseRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @NotNull(message = "Expense amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Expense amount must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimals")
        BigDecimal amount,

        Long categoryId) {
}
