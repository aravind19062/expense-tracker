package com.org.expense.dto;

import com.org.expense.entity.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of POST and PUT /api/expenses: {title, amount, category, expenseDate}.
 * Category is taken as a String and checked against the allowed names, so an unknown value
 * produces a normal validation error (400 VALIDATION_FAILED) instead of a JSON parse error.
 */
public record ExpenseRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimals")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        @Pattern(regexp = Category.PATTERN, message = "Category must be one of FOOD, TRAVEL, BILLS, SHOPPING, OTHER")
        String category,

        @NotNull(message = "Expense date is required")
        @PastOrPresent(message = "Expense date must not be in the future")
        LocalDate expenseDate) {

    public Category categoryEnum() {
        return Category.valueOf(category);
    }
}
