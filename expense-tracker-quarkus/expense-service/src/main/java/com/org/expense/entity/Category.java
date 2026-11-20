package com.org.expense.entity;

/** The fixed set of expense categories from the API spec. Stored by name (EnumType.STRING). */
public enum Category {
    FOOD, TRAVEL, BILLS, SHOPPING, OTHER;

    /** Regex used by Bean Validation on request fields that carry a category name. */
    public static final String PATTERN = "FOOD|TRAVEL|BILLS|SHOPPING|OTHER";
}
