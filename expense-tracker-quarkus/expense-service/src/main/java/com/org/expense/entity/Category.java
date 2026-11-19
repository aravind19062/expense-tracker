package com.org.expense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Migrated from the Spring Boot {@code com.org.expense_service.model.Category}.
 * Same table and columns (category_id, category_name). The inverse
 * {@code @OneToMany expenses} collection was dropped: it was only used for
 * JSON serialization and cascading deletes (see CategoryService#delete).
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    public Long id;

    @Column(name = "category_name", nullable = false, length = 100)
    public String categoryName;
}
