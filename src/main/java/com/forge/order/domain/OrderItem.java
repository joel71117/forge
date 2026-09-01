package com.forge.order.domain;

import com.forge.commerce.common.Money;
import com.forge.commerce.common.Quantity;

import java.util.Objects;
import java.util.UUID;

/**
 * OrderItem keeps a snapshot of the purchase price.
 *
 * <p>That is important: an order should not be affected later by a product price change in
 * the catalog. The item remembers the historical price it was purchased at.</p>
 */
public class OrderItem {
    private OrderItemId id;
    private final UUID productId;
    private final Quantity quantity;
    private final Money unitPrice;

    public OrderItem(UUID productId, Quantity quantity, Money unitPrice) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null.");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null.");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null.");
        }
        this.id = new OrderItemId(UUID.randomUUID());
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem rehydrate(UUID id, UUID productId, Quantity quantity, Money unitPrice) {
        var item = new OrderItem(productId, quantity, unitPrice);
        item.id = new OrderItemId(id);
        return item;
    }

    public OrderItemId getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getSubtotal() {
        return unitPrice.multiply(quantity.value());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem orderItem)) return false;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
