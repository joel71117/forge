package com.forge.order.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class OrderItem {
    private final OrderItemId id;
    private final UUID orderId;
    private final UUID productId;
    private long quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public OrderItem(UUID orderId, UUID productId, long quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = new OrderItemId(UUID.randomUUID());
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }
}
