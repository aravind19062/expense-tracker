package com.org.expense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Expense { id, userId, title, amount, category, expenseDate, createdAt }.
 * Migrated from the Spring Boot {@code com.org.expense_service.model.Expense}; the separate
 * Category table was replaced by a fixed enum, and expenses are owned by the JWT subject (userId).
 */
@Entity
@Table(name = "expense", indexes = {
        @Index(name = "idx_expense_user_id", columnList = "user_id"),
        @Index(name = "idx_expense_user_category", columnList = "user_id, category")})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(nullable = false, length = 100)
    public String title;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Category category;

    @Column(name = "expense_date", nullable = false)
    public LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
