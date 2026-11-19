package com.org.expense.service;

import com.org.expense.dto.ExpenseRequest;
import com.org.expense.entity.Category;
import com.org.expense.entity.Expense;
import com.org.expense.exception.ResourceNotFoundException;
import com.org.expense.repository.CategoryRepository;
import com.org.expense.repository.ExpenseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Migrated from the Spring {@code ExpenseService}. Business rules kept:
 * amount must be > 0, update replaces title/amount/category.
 * All operations are scoped to the owning user (JWT subject).
 */
@ApplicationScoped
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Expense> getAll(String userId) {
        return expenseRepository.findByUser(userId);
    }

    public Expense getById(Long id, String userId) {
        // Another user's expense is reported as "not found" so its existence is not leaked.
        return expenseRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));
    }

    @Transactional
    public Expense create(ExpenseRequest request, String userId) {
        Expense expense = new Expense();
        apply(expense, request);
        expense.userId = userId;
        expenseRepository.persist(expense);
        return expense;
    }

    @Transactional
    public Expense update(Long id, ExpenseRequest request, String userId) {
        Expense expense = getById(id, userId);
        apply(expense, request);
        return expense;
    }

    @Transactional
    public void delete(Long id, String userId) {
        expenseRepository.delete(getById(id, userId));
    }

    private void apply(Expense expense, ExpenseRequest request) {
        // Same guard as the original service; also enforced by Bean Validation at the REST layer.
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }
        expense.title = request.title().trim();
        expense.amount = request.amount();
        expense.category = resolveCategory(request.categoryId());
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdOptional(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
    }
}
