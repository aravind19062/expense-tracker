package com.org.expense.support;

import com.org.expense.repository.CategoryRepository;
import com.org.expense.repository.ExpenseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DbCleaner {

    @Inject
    ExpenseRepository expenses;
    @Inject
    CategoryRepository categories;

    @Transactional
    public void clean() {
        expenses.deleteAll();
        categories.deleteAll();
    }
}
