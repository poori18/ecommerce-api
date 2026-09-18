package com.poornima.ecommerce.service;

import com.poornima.ecommerce.entity.Order;
import com.poornima.ecommerce.entity.OrderItem;
import com.poornima.ecommerce.entity.OrderStatus;
import com.poornima.ecommerce.entity.Payment;
import com.poornima.ecommerce.entity.PaymentMethod;
import com.poornima.ecommerce.entity.PaymentStatus;
import com.poornima.ecommerce.entity.Product;
import com.poornima.ecommerce.exception.BusinessRuleException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
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

    @InjectMocks
    private OrderService orderService;

    @Test
    void should_reduceProductStock_when_orderIsPlaced() {
        // Arrange
        Product product = Product.builder().id(1L).name("Mouse").stockQuantity(10).build();
        OrderItem item = OrderItem.builder()
                .product(Product.builder().id(1L).build())
                .quantity(3)
                .unitPrice(BigDecimal.TEN)
                .build();
        Order order = Order.builder().items(new ArrayList<>(List.of(item))).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.placeOrder(order);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void should_throwBusinessRuleException_when_placingOrderExceedsAvailableStock() {
        // Arrange
        Product product = Product.builder().id(1L).name("Mouse").stockQuantity(2).build();
        OrderItem item = OrderItem.builder()
                .product(Product.builder().id(1L).build())
                .quantity(5)
                .unitPrice(BigDecimal.TEN)
                .build();
        Order order = Order.builder().items(new ArrayList<>(List.of(item))).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(order));
    }

    @Test
    void should_restoreStockAndMarkPaymentRefunded_when_cancellingOrderWithCompletedPayment() {
        // Arrange
        Product product = Product.builder().id(1L).stockQuantity(5).build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        Payment payment = Payment.builder()
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.COMPLETED)
                .amount(BigDecimal.TEN)
                .build();
        Order order = Order.builder()
                .status(OrderStatus.CONFIRMED)
                .items(new ArrayList<>(List.of(item)))
                .payment(payment)
                .build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order result = orderService.cancelOrder(10L);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void should_leavePaymentStatusUnchanged_when_cancellingOrderWithoutCompletedPayment() {
        // Arrange
        Product product = Product.builder().id(1L).stockQuantity(5).build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        Payment payment = Payment.builder()
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.PENDING)
                .amount(BigDecimal.TEN)
                .build();
        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .payment(payment)
                .build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.cancelOrder(10L);

        // Assert
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void should_allowTransition_when_movingFromPendingToConfirmed() {
        // Arrange
        Order order = Order.builder().status(OrderStatus.PENDING).items(new ArrayList<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

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
    void should_throwResourceNotFoundException_when_orderDoesNotExist() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(99L, OrderStatus.CONFIRMED));
    }
}
