package com.poornima.ecommerce.service;

import com.poornima.ecommerce.entity.Order;
import com.poornima.ecommerce.entity.OrderItem;
import com.poornima.ecommerce.entity.OrderStatus;
import com.poornima.ecommerce.entity.Payment;
import com.poornima.ecommerce.entity.PaymentStatus;
import com.poornima.ecommerce.entity.Product;
import com.poornima.ecommerce.exception.BusinessRuleException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.repository.OrderRepository;
import com.poornima.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order placeOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: id=" + item.getProduct().getId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BusinessRuleException(
                        "Insufficient stock for product '" + product.getName() + "'");
            }

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            item.setProduct(product);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: id=" + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessRuleException(
                    "Cannot transition order from " + currentStatus + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
            refundCompletedPayment(order);
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
    }

    private void refundCompletedPayment(Order order) {
        Payment payment = order.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
        }
    }
}
