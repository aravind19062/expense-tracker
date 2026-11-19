package com.org.expense.repository;

import com.org.expense.entity.Expense;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ExpenseRepository implements PanacheRepository<Expense> {

    public List<Expense> findByUser(String userId) {
        // join fetch avoids an N+1 query when mapping category names into the response
        return find("from Expense e left join fetch e.category where e.userId = ?1",
                Sort.by("e.id"), userId).list();
    }

    public Optional<Expense> findByIdAndUser(Long id, String userId) {
        return find("from Expense e left join fetch e.category where e.id = ?1 and e.userId = ?2", id, userId)
                .firstResultOptional();
    }

    public boolean existsByCategory(Long categoryId) {
        return count("category.id", categoryId) > 0;
    }
}
