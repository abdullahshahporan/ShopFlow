package com.shahporan.demo.repository;

import com.shahporan.demo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity operations.
 * Provides CRUD operations and custom queries for managing product categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find all active categories.
     */
    List<Category> findByActiveTrue();

    /**
     * Find category by name.
     */
    Optional<Category> findByName(String name);

    /**
     * Find category by name (case-insensitive).
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Check if a category name exists.
     */
    boolean existsByName(String name);

    /**
     * Check if a category name exists (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if a category name exists for a different category ID (for updates).
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Find active category by ID.
     */
    Optional<Category> findByIdAndActiveTrue(Long id);
}
