package com.shahporan.demo.service;

import com.shahporan.demo.entity.Category;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for managing product categories.
 * Provides business logic for category CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get all categories.
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Get all active categories.
     */
    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    /**
     * Get category by ID.
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    /**
     * Get category by name.
     */
    public Category getCategoryByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
    }

    /**
     * Create a new category.
     */
    @Transactional
    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Category with name '" + name + "' already exists");
        }

        Category category = Category.builder()
                .name(name)
                .description(description)
                .active(true)
                .build();

        return categoryRepository.save(category);
    }

    /**
     * Update an existing category.
     */
    @Transactional
    public Category updateCategory(Long id, String name, String description, Boolean active) {
        Category category = getCategoryById(id);

        // Check if name is being changed and if new name already exists
        if (name != null && !name.equalsIgnoreCase(category.getName())) {
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new IllegalArgumentException("Category with name '" + name + "' already exists");
            }
            category.setName(name);
        }

        if (description != null) {
            category.setDescription(description);
        }

        if (active != null) {
            category.setActive(active);
        }

        return categoryRepository.save(category);
    }

    /**
     * Delete a category by ID.
     * Note: This will remove the category from all associated products due to cascade settings.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }

    /**
     * Toggle category active status.
     */
    @Transactional
    public Category toggleCategoryStatus(Long id) {
        Category category = getCategoryById(id);
        category.setActive(!category.getActive());
        return categoryRepository.save(category);
    }

    /**
     * Check if category exists by name.
     */
    public boolean existsByName(String name) {
        return categoryRepository.existsByNameIgnoreCase(name);
    }

    /**
     * Count total categories.
     */
    public long countCategories() {
        return categoryRepository.count();
    }

    /**
     * Count active categories.
     */
    public long countActiveCategories() {
        return categoryRepository.findByActiveTrue().size();
    }
}
