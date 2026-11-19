package com.org.expense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Migrated from the Spring Boot {@code com.org.expense_service.model.Expense}.
 * Fields title/amount/category keep their original column definitions.
 * New: {@code userId} - the JWT subject of the owner, so users only see their own expenses.
 */
@Entity
@Table(name = "expense", indexes = @Index(name = "idx_expense_user_id", columnList = "user_id"))
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String title;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public Category category;

    @Column(name = "user_id", nullable = false)
    public String userId;
}
