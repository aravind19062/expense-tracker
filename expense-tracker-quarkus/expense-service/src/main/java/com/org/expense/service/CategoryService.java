package com.org.expense.service;

import com.org.expense.dto.CategoryRequest;
import com.org.expense.entity.Category;
import com.org.expense.exception.ConflictException;
import com.org.expense.exception.ResourceNotFoundException;
import com.org.expense.repository.CategoryRepository;
import com.org.expense.repository.ExpenseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public CategoryService(CategoryRepository categoryRepository, ExpenseRepository expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<Category> getAll() {
        return categoryRepository.listAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Transactional
    public Category create(CategoryRequest request) {
        Category category = new Category();
        category.categoryName = request.categoryName().trim();
        categoryRepository.persist(category);
        return category;
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);
        category.categoryName = request.categoryName().trim();
        return category; // managed entity, flushed on commit
    }

    /**
     * The Spring version cascaded the delete to every expense in the category (CascadeType.ALL).
     * Now that expenses belong to individual users, that would delete other users' data,
     * so deleting a category that is still in use is rejected with 409 instead.
     */
    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        if (expenseRepository.existsByCategory(id)) {
            throw new ConflictException("Category " + id + " is still used by one or more expenses");
        }
        categoryRepository.delete(category);
    }
}
