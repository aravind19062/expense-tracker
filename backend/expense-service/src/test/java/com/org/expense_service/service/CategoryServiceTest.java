package com.org.expense_service.service;

import com.org.expense_service.model.Category;
import com.org.expense_service.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryService categoryService;

    @Test
    void testAddCategory(){
        Category category=new Category();
        category.setCategoryName("food");

        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        Category result = categoryService.addCategory(category);

        assertNotNull(result);
        assertEquals("food", result.getCategoryName());
        verify(categoryRepository, times(1)).save(category);
    }
    @Test
    void testGetAllCategories(){
        Category category=new Category();
        category.setCategoryName("food");

        Category category1=new Category();
        category1.setCategoryName("gym");

        List<Category> list = Arrays.asList(category, category1);
        when(categoryRepository.findAll()).thenReturn(list);
        List<Category> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
        assertEquals("food", result.get(0).getCategoryName());
        verify(categoryRepository, times(1)).findAll();

    }
    @Test
    void testGetCategoryById(){
        Long categoryId = 1L;
        Category category = new Category();
        category.setCategory_id(categoryId);
        category.setCategoryName("food");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        Category result = categoryService.getCategoryById(categoryId);
        assertNotNull(result);
        assertEquals("food",result.getCategoryName());
        assertEquals(1L, result.getCategory_id());
        verify(categoryRepository,times(1)).findById(categoryId);
    }
    @Test
    void testGetCategoryByIdThrowsException(){
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, ()->{
            categoryService.getCategoryById(categoryId);
        });
        verify(categoryRepository, times(1)).findById(categoryId);
    }
    @Test
    void testUpdateCategoryById(){
        Long categoryId = 1L;
        Category category = new Category();
        category.setCategory_id(categoryId);
        category.setCategoryName("entertainment");

        Category updatedCategory  = new Category();
        updatedCategory.setCategory_id(categoryId);
        updatedCategory.setCategoryName("ENT");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        Category result = categoryService.updateCategoryById(categoryId,updatedCategory);
        assertEquals("ENT", result.getCategoryName());
        assertEquals(categoryId,result.getCategory_id());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }
    @Test
    void testUpdateCategoryByIdInvalidId(){
        Long categoryId = 1L;
        Category category=new Category();
        category.setCategory_id(Long.valueOf(2L));
        category.setCategoryName("entertainment");

        Category newCategory= new Category();
        newCategory.setCategory_id(categoryId);
        newCategory.setCategoryName("ENT");

        when(categoryRepository.findById(category.getCategory_id())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, ()->{
            categoryService.updateCategoryById(category.getCategory_id(), newCategory);
        });
        verify(categoryRepository,times(1)).findById(category.getCategory_id());
    }
    @Test
    void testDeleteCategoryById(){
        Long categoryId = 1L;
        Category category = new Category();
        category.setCategory_id(categoryId);
        category.setCategoryName("food");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.deleteCategoryById(categoryId);
        verify(categoryRepository, times(1)).findById(categoryId);

    }
    @Test
    void testDeleteCategoryByInvalidId(){
        Long categoryId = 1L;
        Category category=new Category();
        category.setCategory_id(categoryId);
        category.setCategoryName("ENT");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, ()->{
            categoryService.deleteCategoryById(categoryId);
        });

        verify(categoryRepository, times(1)).findById(categoryId);
    }




}
