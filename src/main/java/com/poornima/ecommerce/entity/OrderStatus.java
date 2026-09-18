package com.poornima.ecommerce.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PENDING, EnumSet.of(CONFIRMED, CANCELLED));
        ALLOWED_TRANSITIONS.put(CONFIRMED, EnumSet.of(PROCESSING, CANCELLED));
        ALLOWED_TRANSITIONS.put(PROCESSING, EnumSet.of(SHIPPED, CANCELLED));
        ALLOWED_TRANSITIONS.put(SHIPPED, EnumSet.of(DELIVERED));
        ALLOWED_TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
