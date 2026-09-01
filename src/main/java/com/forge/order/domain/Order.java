package com.forge.order.domain;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.commerce.common.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order is the aggregate root for order behavior.
 *
 * <p>The state machine is enforced inside the aggregate so callers cannot set arbitrary
 * statuses and accidentally break invariants. The list is also kept read-only from the outside.</p>
 */
public class Order {
    private OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items = new ArrayList<>();
    private final Currency currency;
    private final IdempotencyKey idempotencyKey;
    private OrderStatus status;

    public Order(CustomerId customerId, Currency currency, IdempotencyKey idempotencyKey) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId cannot be null.");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key cannot be null.");
        }
        this.id = new OrderId(UUID.randomUUID());
        this.customerId = customerId;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.CREATED;
    }

    public static Order rehydrate(UUID id, UUID customerId, Currency currency, String idempotencyKey,
            OrderStatus status, List<OrderItem> items) {
        var order = new Order(new CustomerId(customerId), currency, new IdempotencyKey(idempotencyKey));
        order.id = new OrderId(id);
        order.status = status;
        order.items.addAll(items);
        return order;
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Currency getCurrency() {
        return currency;
    }

    public IdempotencyKey getIdempotencyKey() {
        return idempotencyKey;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null.");
        }
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Items can only be added while the order is in CREATED state.");
        }
        items.add(item);
    }

    public void removeItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null.");
        }
        items.remove(item);
    }

    public Money calculateTotal() {
        Money total = Money.of("0.00", currency);
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    public void confirm() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Order cannot be confirmed without items.");
        }
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Only a CREATED order can be confirmed.");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void startProcessing() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only a CONFIRMED order can start processing.");
        }
        this.status = OrderStatus.PROCESSING;
    }

    public void complete() {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Only a PROCESSING order can be completed.");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void fail() {
        if (status != OrderStatus.PROCESSING && status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order can only fail while processing or confirming.");
        }
        this.status = OrderStatus.FAILED;
    }

    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Only a CREATED order can be cancelled.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
