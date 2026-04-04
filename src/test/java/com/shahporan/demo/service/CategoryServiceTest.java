package com.shahporan.demo.service;

import com.shahporan.demo.entity.Category;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryService.
 * Tests the Many-to-Many relationship functionality for Product ↔ Category.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category electronicsCategory;
    private Category clothingCategory;

    @BeforeEach
    void setUp() {
        electronicsCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        clothingCategory = Category.builder()
                .id(2L)
                .name("Clothing")
                .description("Fashion and apparel")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // getAllCategories
    // -----------------------------------------------------------------------

    @Test
    void getAllCategories_thenReturnsAllCategories() {
        List<Category> categories = Arrays.asList(electronicsCategory, clothingCategory);
        when(categoryRepository.findAll()).thenReturn(categories);

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(electronicsCategory, clothingCategory);
        verify(categoryRepository).findAll();
    }

    // -----------------------------------------------------------------------
    // getActiveCategories
    // -----------------------------------------------------------------------

    @Test
    void getActiveCategories_thenReturnsOnlyActiveCategories() {
        List<Category> activeCategories = List.of(electronicsCategory);
        when(categoryRepository.findByActiveTrue()).thenReturn(activeCategories);

        List<Category> result = categoryService.getActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
        verify(categoryRepository).findByActiveTrue();
    }

    // -----------------------------------------------------------------------
    // getCategoryById
    // -----------------------------------------------------------------------

    @Test
    void getCategoryById_whenCategoryExists_thenReturnsCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));

        Category result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository).findById(1L);
    }

    @Test
    void getCategoryById_whenCategoryNotFound_thenThrowsResourceNotFoundException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(999L));
    }

    // -----------------------------------------------------------------------
    // getCategoryByName
    // -----------------------------------------------------------------------

    @Test
    void getCategoryByName_whenCategoryExists_thenReturnsCategory() {
        when(categoryRepository.findByNameIgnoreCase("Electronics"))
                .thenReturn(Optional.of(electronicsCategory));

        Category result = categoryService.getCategoryByName("Electronics");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository).findByNameIgnoreCase("Electronics");
    }

    @Test
    void getCategoryByName_whenCategoryNotFound_thenThrowsResourceNotFoundException() {
        when(categoryRepository.findByNameIgnoreCase("NonExistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryByName("NonExistent"));
    }

    // -----------------------------------------------------------------------
    // createCategory
    // -----------------------------------------------------------------------

    @Test
    void createCategory_whenValidData_thenCreatesCategory() {
        when(categoryRepository.existsByNameIgnoreCase("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(3L);
            return cat;
        });

        Category result = categoryService.createCategory("Books", "Books and literature");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Books");
        assertThat(result.getDescription()).isEqualTo("Books and literature");
        assertThat(result.getActive()).isTrue();
        verify(categoryRepository).existsByNameIgnoreCase("Books");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_whenNameAlreadyExists_thenThrowsIllegalArgumentException() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.createCategory("Electronics", "Duplicate"));

        verify(categoryRepository).existsByNameIgnoreCase("Electronics");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // -----------------------------------------------------------------------
    // updateCategory
    // -----------------------------------------------------------------------

    @Test
    void updateCategory_whenValidData_thenUpdatesCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.updateCategory(1L, "Consumer Electronics",
                "Updated description", true);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Consumer Electronics");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(electronicsCategory);
    }

    @Test
    void updateCategory_whenNameConflicts_thenThrowsIllegalArgumentException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Clothing", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.updateCategory(1L, "Clothing", "Change name", true));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_whenCategoryNotFound_thenThrowsResourceNotFoundException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(999L, "New Name", "Desc", true));
    }

    // -----------------------------------------------------------------------
    // deleteCategory
    // -----------------------------------------------------------------------

    @Test
    void deleteCategory_whenCategoryExists_thenDeletesCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).delete(electronicsCategory);
    }

    @Test
    void deleteCategory_whenCategoryNotFound_thenThrowsResourceNotFoundException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(999L));

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    // -----------------------------------------------------------------------
    // toggleCategoryStatus
    // -----------------------------------------------------------------------

    @Test
    void toggleCategoryStatus_whenCategoryIsActive_thenDeactivates() {
        electronicsCategory.setActive(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.toggleCategoryStatus(1L);

        assertThat(result.getActive()).isFalse();
        verify(categoryRepository).save(electronicsCategory);
    }

    @Test
    void toggleCategoryStatus_whenCategoryIsInactive_thenActivates() {
        electronicsCategory.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.toggleCategoryStatus(1L);

        assertThat(result.getActive()).isTrue();
        verify(categoryRepository).save(electronicsCategory);
    }

    // -----------------------------------------------------------------------
    // existsByName
    // -----------------------------------------------------------------------

    @Test
    void existsByName_whenExists_thenReturnsTrue() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        boolean exists = categoryService.existsByName("Electronics");

        assertThat(exists).isTrue();
        verify(categoryRepository).existsByNameIgnoreCase("Electronics");
    }

    @Test
    void existsByName_whenNotExists_thenReturnsFalse() {
        when(categoryRepository.existsByNameIgnoreCase("NonExistent")).thenReturn(false);

        boolean exists = categoryService.existsByName("NonExistent");

        assertThat(exists).isFalse();
        verify(categoryRepository).existsByNameIgnoreCase("NonExistent");
    }

    // -----------------------------------------------------------------------
    // countCategories
    // -----------------------------------------------------------------------

    @Test
    void countCategories_thenReturnsCount() {
        when(categoryRepository.count()).thenReturn(5L);

        long count = categoryService.countCategories();

        assertThat(count).isEqualTo(5L);
        verify(categoryRepository).count();
    }

    // -----------------------------------------------------------------------
    // countActiveCategories
    // -----------------------------------------------------------------------

    @Test
    void countActiveCategories_thenReturnsActiveCount() {
        List<Category> activeCategories = Arrays.asList(electronicsCategory, clothingCategory);
        when(categoryRepository.findByActiveTrue()).thenReturn(activeCategories);

        long count = categoryService.countActiveCategories();

        assertThat(count).isEqualTo(2L);
        verify(categoryRepository).findByActiveTrue();
    }
}
