package com.org.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Migrated from the Spring {@code com.example.auth_service.model.User} (table "users").
 * Column names match Spring's default snake_case naming. New: {@code role}.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_name", nullable = false, length = 100)
    public String userName;

    @Column(nullable = false, unique = true)
    public String email;

    /** BCrypt hash in Modular Crypt Format ($2a$...). Never the plain-text password. */
    @Column(nullable = false, length = 60)
    public String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Role role = Role.USER;
}
