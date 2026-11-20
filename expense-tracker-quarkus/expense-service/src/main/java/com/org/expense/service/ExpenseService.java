package com.org.expense.service;

import com.org.expense.dto.ExpenseRequest;
import com.org.expense.entity.Category;
import com.org.expense.entity.Expense;
import com.org.expense.exception.ResourceNotFoundException;
import com.org.expense.repository.ExpenseRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business logic for expenses. Every operation is scoped to the owning user (the JWT subject). */
@ApplicationScoped
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public record PageResult(List<Expense> items, long totalCount) {
    }

    public PageResult list(String userId, Category category, int page, int size) {
        PanacheQuery<Expense> query = repository.findPage(userId, category, page, size);
        return new PageResult(query.list(), query.count());
    }

    public List<Expense> listAll(String userId) {
        return repository.findAllByUser(userId);
    }

    public Expense getById(Long id, String userId) {
        // Another user's expense is reported as "not found" so its existence is not leaked.
        return repository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense " + id + " not found"));
    }

    @Transactional
    public Expense create(ExpenseRequest request, String userId) {
        Expense expense = new Expense();
        expense.userId = userId;
        apply(expense, request);
        repository.persist(expense);
        return expense;
    }

    /** PUT replaces every editable field (title, amount, category, expenseDate). */
    @Transactional
    public Expense update(Long id, ExpenseRequest request, String userId) {
        Expense expense = getById(id, userId);
        apply(expense, request);
        return expense;
    }

    @Transactional
    public void delete(Long id, String userId) {
        repository.delete(getById(id, userId));
    }

    /** {category: total} for every category, in enum order; categories with no expenses report 0.00. */
    public Map<Category, BigDecimal> summary(String userId) {
        Map<Category, BigDecimal> totals = repository.totalsByCategory(userId);
        Map<Category, BigDecimal> result = new LinkedHashMap<>();
        for (Category c : Category.values()) {
            result.put(c, totals.getOrDefault(c, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private static void apply(Expense expense, ExpenseRequest request) {
        expense.title = request.title().trim();
        expense.amount = request.amount();
        expense.category = request.categoryEnum();
        expense.expenseDate = request.expenseDate();
    }
}
