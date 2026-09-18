package com.poornima.ecommerce.service;

import com.poornima.ecommerce.dto.OrderItemRequest;
import com.poornima.ecommerce.dto.OrderRequest;
import com.poornima.ecommerce.dto.OrderResponse;
import com.poornima.ecommerce.entity.Customer;
import com.poornima.ecommerce.entity.Order;
import com.poornima.ecommerce.entity.OrderItem;
import com.poornima.ecommerce.entity.OrderStatus;
import com.poornima.ecommerce.entity.Payment;
import com.poornima.ecommerce.entity.PaymentMethod;
import com.poornima.ecommerce.entity.PaymentStatus;
import com.poornima.ecommerce.entity.Product;
import com.poornima.ecommerce.exception.BusinessRuleException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.repository.CustomerRepository;
import com.poornima.ecommerce.repository.OrderRepository;
import com.poornima.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void should_reduceProductStock_when_orderIsPlaced() {
        // Arrange
        Customer customer = Customer.builder().id(1L).build();
        Product product = Product.builder().id(10L).name("Mouse").price(BigDecimal.TEN).stockQuantity(10).build();
        OrderRequest request = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Street")
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(3).build()))
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.placeOrder(request);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void should_throwBusinessRuleException_when_placingOrderExceedsAvailableStock() {
        // Arrange
        Customer customer = Customer.builder().id(1L).build();
        Product product = Product.builder().id(10L).name("Mouse").price(BigDecimal.TEN).stockQuantity(2).build();
        OrderRequest request = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Street")
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(5).build()))
                .build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(request));
    }

    @Test
    void should_throwResourceNotFoundException_when_placingOrderForUnknownCustomer() {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .customerId(99L)
                .shippingAddress("123 Street")
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(1).build()))
                .build();
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(request));
    }

    @Test
    void should_restoreStockAndMarkPaymentRefunded_when_cancellingOrderWithCompletedPayment() {
        // Arrange
        Customer customer = Customer.builder().id(1L).build();
        Product product = Product.builder().id(1L).stockQuantity(5).build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        Payment payment = Payment.builder()
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.COMPLETED)
                .amount(BigDecimal.TEN)
                .build();
        Order order = Order.builder()
                .status(OrderStatus.CONFIRMED)
                .customer(customer)
                .items(new ArrayList<>(List.of(item)))
                .payment(payment)
                .build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrderStatus(10L, OrderStatus.CANCELLED);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void should_leavePaymentStatusUnchanged_when_cancellingOrderWithoutCompletedPayment() {
        // Arrange
        Customer customer = Customer.builder().id(1L).build();
        Product product = Product.builder().id(1L).stockQuantity(5).build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        Payment payment = Payment.builder()
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.PENDING)
                .amount(BigDecimal.TEN)
                .build();
        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .customer(customer)
                .items(new ArrayList<>(List.of(item)))
                .payment(payment)
                .build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.updateOrderStatus(10L, OrderStatus.CANCELLED);

        // Assert
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void should_allowTransition_when_movingFromPendingToConfirmed() {
        // Arrange
        Customer customer = Customer.builder().id(1L).build();
        Order order = Order.builder().status(OrderStatus.PENDING).customer(customer).items(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void should_throwBusinessRuleException_when_transitionSkipsRequiredStates() {
        // Arrange
        Order order = Order.builder().status(OrderStatus.PENDING).items(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED));
    }

    @Test
    void should_throwBusinessRuleException_when_transitioningFromFinalState() {
        // Arrange
        Order order = Order.builder().status(OrderStatus.DELIVERED).items(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED));
    }

    @Test
    void should_throwBusinessRuleException_when_cancellingAlreadyDeliveredOrder() {
        // Arrange
        Order order = Order.builder().status(OrderStatus.DELIVERED).items(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED));
    }

    @Test
    void should_throwBusinessRuleException_when_cancellingAlreadyCancelledOrder() {
        // Arrange
        Order order = Order.builder().status(OrderStatus.CANCELLED).items(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED));
    }

    @Test
    void should_throwResourceNotFoundException_when_orderDoesNotExist() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(99L, OrderStatus.CONFIRMED));
    }
}
