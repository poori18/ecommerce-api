package com.poornima.ecommerce.service;

import com.poornima.ecommerce.dto.CategoryRequest;
import com.poornima.ecommerce.dto.CategoryResponse;
import com.poornima.ecommerce.entity.Category;
import com.poornima.ecommerce.exception.DuplicateResourceException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void should_createCategory_when_nameAndSlugAreUnique() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic goods")
                .build();
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse result = categoryService.create(request);

        // Assert
        assertThat(result.getName()).isEqualTo("Electronics");
        assertThat(result.getSlug()).isEqualTo("electronics");
    }

    @Test
    void should_throwDuplicateResourceException_when_creatingCategoryWithExistingName() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .build();
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> categoryService.create(request));
    }

    @Test
    void should_throwDuplicateResourceException_when_creatingCategoryWithExistingSlug() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .build();
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> categoryService.create(request));
    }

    @Test
    void should_throwResourceNotFoundException_when_categoryDoesNotExist() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(99L));
    }

    @Test
    void should_updateCategory_when_categoryExists() {
        // Arrange
        Category existing = Category.builder().id(1L).name("Electronics").slug("electronics").build();
        CategoryRequest request = CategoryRequest.builder()
                .name("Gadgets")
                .slug("gadgets")
                .description("Updated description")
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Gadgets")).thenReturn(false);
        when(categoryRepository.existsBySlug("gadgets")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse result = categoryService.update(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Gadgets");
    }

    @Test
    void should_deleteCategory_when_categoryExists() {
        // Arrange
        Category existing = Category.builder().id(1L).name("Electronics").slug("electronics").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        categoryService.delete(1L);

        // Assert
        verify(categoryRepository).delete(existing);
    }
}
