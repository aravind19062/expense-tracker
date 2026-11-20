package com.org.expense.repository;

import com.org.expense.entity.Category;
import com.org.expense.entity.Expense;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ExpenseRepository implements PanacheRepository<Expense> {

    private static final Sort NEWEST_FIRST = Sort.by("expenseDate", Sort.Direction.Descending)
            .and("id", Sort.Direction.Descending);

    /** One page of the user's expenses, optionally filtered by category, newest first. */
    public PanacheQuery<Expense> findPage(String userId, Category category, int page, int size) {
        PanacheQuery<Expense> query = category == null
                ? find("userId = :user", NEWEST_FIRST, Parameters.with("user", userId))
                : find("userId = :user and category = :category", NEWEST_FIRST,
                        Parameters.with("user", userId).and("category", category));
        return query.page(Page.of(page, size));
    }

    public List<Expense> findAllByUser(String userId) {
        return find("userId", NEWEST_FIRST, userId).list();
    }

    public Optional<Expense> findByIdAndUser(Long id, String userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    /** Total amount per category for one user, computed in the database. Categories without expenses are absent. */
    public Map<Category, BigDecimal> totalsByCategory(String userId) {
        List<Object[]> rows = getEntityManager()
                .createQuery("select e.category, sum(e.amount) from Expense e where e.userId = :user group by e.category",
                        Object[].class)
                .setParameter("user", userId)
                .getResultList();
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Object[] row : rows) {
            totals.put((Category) row[0], (BigDecimal) row[1]);
        }
        return totals;
    }
}
