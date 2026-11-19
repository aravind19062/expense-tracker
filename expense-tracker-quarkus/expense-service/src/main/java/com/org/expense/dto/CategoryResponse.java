package com.org.expense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.org.expense.entity.Category;

/** Keeps the original JSON property name {@code category_id} for compatibility. */
public record CategoryResponse(@JsonProperty("category_id") Long categoryId, String categoryName) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id, c.categoryName);
    }
}
