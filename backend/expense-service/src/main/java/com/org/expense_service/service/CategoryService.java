package com.org.expense_service.service;

import com.org.expense_service.model.Category;
import com.org.expense_service.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category addCategory(Category category){
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id){
        return categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("given id is not there in database"+ id));
    }

    public Category updateCategoryById(Long id, Category updatedCategory){
        Category category = categoryRepository.findById(id).orElseThrow(()->new RuntimeException("given id is not available in database"+ id));
        category.setCategoryName(updatedCategory.getCategoryName());
        return categoryRepository.save(category);
    }

    public void deleteCategoryById(Long id){
        Category category = categoryRepository.findById(id).orElseThrow(()->new RuntimeException("this id doesnt exist in database" + id));
        categoryRepository.deleteById(id);
    }
}
