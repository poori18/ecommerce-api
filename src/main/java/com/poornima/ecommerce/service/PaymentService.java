package com.poornima.ecommerce.service;

import com.poornima.ecommerce.dto.PaymentRequest;
import com.poornima.ecommerce.dto.PaymentResponse;
import com.poornima.ecommerce.entity.Order;
import com.poornima.ecommerce.entity.Payment;
import com.poornima.ecommerce.entity.PaymentStatus;
import com.poornima.ecommerce.exception.BusinessRuleException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.repository.OrderRepository;
import com.poornima.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse recordPayment(Long orderId, PaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: id=" + orderId));

        if (order.getPayment() != null) {
            throw new BusinessRuleException("Order already has a payment recorded: id=" + orderId);
        }

        Payment payment = Payment.builder()
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .amount(request.getAmount())
                .build();

        order.setPayment(payment);
        orderRepository.save(order);

        return toResponse(order.getPayment());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PaymentResponse updateStatus(Long id, PaymentStatus newStatus) {
        Payment payment = findOrThrow(id);
        payment.setPaymentStatus(newStatus);
        return toResponse(paymentRepository.save(payment));
    }

    private Payment findOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: id=" + id));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
