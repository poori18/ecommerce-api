package com.poornima.ecommerce.service;

import com.poornima.ecommerce.dto.ProductRequest;
import com.poornima.ecommerce.dto.ProductResponse;
import com.poornima.ecommerce.entity.Category;
import com.poornima.ecommerce.entity.Product;
import com.poornima.ecommerce.exception.DuplicateResourceException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.repository.CategoryRepository;
import com.poornima.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void should_createProduct_when_skuIsUnique() {
        // Arrange
        Category category = Category.builder().id(1L).name("Electronics").build();
        ProductRequest request = ProductRequest.builder()
                .name("Mouse")
                .price(BigDecimal.TEN)
                .sku("SKU-001")
                .stockQuantity(10)
                .categoryId(1L)
                .build();
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse result = productService.create(request);

        // Assert
        assertThat(result.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void should_throwDuplicateResourceException_when_creatingProductWithExistingSku() {
        // Arrange
        ProductRequest request = ProductRequest.builder()
                .name("Mouse")
                .price(BigDecimal.TEN)
                .sku("SKU-001")
                .stockQuantity(10)
                .categoryId(1L)
                .build();
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> productService.create(request));
    }

    @Test
    void should_throwResourceNotFoundException_when_creatingProductForUnknownCategory() {
        // Arrange
        ProductRequest request = ProductRequest.builder()
                .name("Mouse")
                .price(BigDecimal.TEN)
                .sku("SKU-001")
                .stockQuantity(10)
                .categoryId(99L)
                .build();
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productService.create(request));
    }

    @Test
    void should_throwResourceNotFoundException_when_productDoesNotExist() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productService.getById(99L));
    }

    @Test
    void should_deleteProduct_when_productExists() {
        // Arrange
        Category category = Category.builder().id(1L).name("Electronics").build();
        Product existing = Product.builder().id(1L).name("Mouse").sku("SKU-001").category(category).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        productService.delete(1L);

        // Assert
        verify(productRepository).delete(existing);
    }
}
