package com.poornima.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotBlank(message = "shippingAddress is required")
    private String shippingAddress;

    private String notes;

    @NotEmpty(message = "items must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;
}
