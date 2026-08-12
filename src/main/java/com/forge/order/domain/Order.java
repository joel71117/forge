package com.forge.order.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Order {
    private final OrderId id;
    private final UUID customerId;
    private OrderStatus status;
    private String currency;
    private BigDecimal totalAmount;
    private String idempotencyKey;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(UUID customerId, OrderStatus status, String currency, BigDecimal totalAmount,
                 String idempotencyKey, Instant createdAt, Instant updatedAt) {
        this.id = new OrderId(UUID.randomUUID());
        this.customerId = customerId;
        this.status = status;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
