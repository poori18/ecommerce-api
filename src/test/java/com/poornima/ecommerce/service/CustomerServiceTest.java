package com.poornima.ecommerce.service;

import com.poornima.ecommerce.dto.CustomerRequest;
import com.poornima.ecommerce.dto.CustomerResponse;
import com.poornima.ecommerce.entity.Customer;
import com.poornima.ecommerce.exception.DuplicateResourceException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.repository.CustomerRepository;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void should_registerCustomer_when_emailIsUnique() {
        // Arrange
        CustomerRequest request = CustomerRequest.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .build();
        when(customerRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CustomerResponse result = customerService.create(request);

        // Assert
        assertThat(result.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void should_throwDuplicateResourceException_when_registeringCustomerWithExistingEmail() {
        // Arrange
        CustomerRequest request = CustomerRequest.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .build();
        when(customerRepository.existsByEmail("ada@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> customerService.create(request));
    }

    @Test
    void should_throwResourceNotFoundException_when_customerDoesNotExist() {
        // Arrange
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> customerService.getById(99L));
    }

    @Test
    void should_updateCustomer_when_customerExists() {
        // Arrange
        Customer existing = Customer.builder().id(1L).firstName("Ada").lastName("Lovelace").email("ada@example.com").build();
        CustomerRequest request = CustomerRequest.builder()
                .firstName("Ada")
                .lastName("Byron")
                .email("ada@example.com")
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CustomerResponse result = customerService.update(1L, request);

        // Assert
        assertThat(result.getLastName()).isEqualTo("Byron");
    }

    @Test
    void should_deleteCustomer_when_customerExists() {
        // Arrange
        Customer existing = Customer.builder().id(1L).firstName("Ada").lastName("Lovelace").email("ada@example.com").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        customerService.delete(1L);

        // Assert
        verify(customerRepository).delete(existing);
    }
}
