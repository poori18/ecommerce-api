package com.poornima.ecommerce.controller;

import com.poornima.ecommerce.dto.PaymentRequest;
import com.poornima.ecommerce.dto.PaymentResponse;
import com.poornima.ecommerce.dto.PaymentStatusUpdateRequest;
import com.poornima.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/v1/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@PathVariable Long orderId,
                                                           @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.recordPayment(orderId, request));
    }

    @GetMapping("/api/v1/payments/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PatchMapping("/api/v1/payments/{id}/status")
    public ResponseEntity<PaymentResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return ResponseEntity.ok(paymentService.updateStatus(id, request.getStatus()));
    }
}
