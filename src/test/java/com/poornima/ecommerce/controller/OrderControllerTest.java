package com.poornima.ecommerce.controller;

import com.poornima.ecommerce.dto.OrderItemRequest;
import com.poornima.ecommerce.dto.OrderItemResponse;
import com.poornima.ecommerce.dto.OrderRequest;
import com.poornima.ecommerce.dto.OrderResponse;
import com.poornima.ecommerce.dto.OrderStatusUpdateRequest;
import com.poornima.ecommerce.entity.OrderStatus;
import com.poornima.ecommerce.exception.BusinessRuleException;
import com.poornima.ecommerce.exception.ResourceNotFoundException;
import com.poornima.ecommerce.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(JacksonAutoConfiguration.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void should_returnCreated_when_placingValidOrder() throws Exception {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Street")
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
                .build();
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-1")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(20))
                .shippingAddress("123 Street")
                .items(List.of(OrderItemResponse.builder().productId(10L).quantity(2).build()))
                .build();
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORD-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void should_returnBadRequest_when_placingOrderWithoutItems() throws Exception {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Street")
                .items(List.of())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void should_returnNotFound_when_placingOrderForUnknownCustomer() throws Exception {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .customerId(99L)
                .shippingAddress("123 Street")
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(1).build()))
                .build();
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99"));
    }

    @Test
    void should_returnUnprocessableEntity_when_placingOrderExceedsAvailableStock() throws Exception {
        // Arrange
        OrderRequest request = OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Street")
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(50).build()))
                .build();
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new BusinessRuleException("Insufficient stock for product: Mouse"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Insufficient stock for product: Mouse"));
    }

    @Test
    void should_returnOkWithOrderList_when_gettingAllOrders() throws Exception {
        // Arrange
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-1")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(20))
                .build();
        when(orderService.getAll()).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-1"));
    }

    @Test
    void should_returnOkWithOrder_when_orderExists() throws Exception {
        // Arrange
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-1")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(20))
                .build();
        when(orderService.getById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void should_returnNotFound_when_orderDoesNotExist() throws Exception {
        // Arrange
        when(orderService.getById(99L)).thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
    }

    @Test
    void should_returnOkWithUpdatedOrder_when_statusTransitionIsAllowed() throws Exception {
        // Arrange
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder().status(OrderStatus.CONFIRMED).build();
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-1")
                .customerId(1L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(20))
                .build();
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void should_returnBadRequest_when_updatingStatusWithoutStatusField() throws Exception {
        // Arrange
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder().status(null).build();

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void should_returnNotFound_when_updatingStatusForUnknownOrder() throws Exception {
        // Arrange
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder().status(OrderStatus.CONFIRMED).build();
        when(orderService.updateOrderStatus(eq(99L), any(OrderStatus.class)))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/{id}/status", 99L)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
    }

    @Test
    void should_returnUnprocessableEntity_when_statusTransitionSkipsRequiredStates() throws Exception {
        // Arrange
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder().status(OrderStatus.SHIPPED).build();
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.SHIPPED)))
                .thenThrow(new BusinessRuleException("Cannot transition order from PENDING to SHIPPED"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Cannot transition order from PENDING to SHIPPED"));
    }
}
