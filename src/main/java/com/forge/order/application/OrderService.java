package com.forge.order.application;

import com.forge.common.api.ConflictException;
import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.commerce.common.Currency;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Quantity;
import com.forge.order.application.port.OrderRepository;
import com.forge.order.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import com.forge.infrastructure.idempotency.IdempotencyRequestStore;
import com.forge.infrastructure.idempotency.RequestFingerprint;

import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final EventPublisher eventPublisher;
    private final ObjectProvider<IdempotencyRequestStore> idempotencyStore;

    public OrderService(OrderRepository repository, EventPublisher eventPublisher,
            ObjectProvider<IdempotencyRequestStore> idempotencyStore) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.idempotencyStore = idempotencyStore;
    }

    @Transactional
    public Order create(String idempotencyKey, String customerId, String currency,
            java.util.List<com.forge.order.api.dto.OrderItemRequest> items) {
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Idempotency-Key is required");
        var requestHash = RequestFingerprint.sha256(customerId + "|" + currency + "|" + items.toString());
        var store = idempotencyStore.getIfAvailable();
        if (store != null) {
            var completedId = store.reserve("ORDER_CREATE", idempotencyKey, requestHash);
            if (completedId != null) return get(completedId);
        }
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent())
            return existing.get();
        var order = new Order(new CustomerId(UUID.fromString(customerId)), Currency.valueOf(currency),
                new IdempotencyKey(idempotencyKey));
        for (var item : items) {
            order.addItem(new OrderItem(UUID.fromString(item.productId), new Quantity(item.quantity),
                    new Money(item.unitPrice, order.getCurrency())));
        }
        order.confirm();
        repository.save(order);
        eventPublisher.publish(new EventEnvelope("OrderConfirmed", order.getId().toString(), "Order",
            java.util.Map.of("orderId", order.getId().toString(), "customerId", order.getCustomerId().toString(),
                "status", order.getStatus().name(), "totalAmount", order.calculateTotal().amount(),
                "itemCount", order.getItems().size(), "aggregateVersion", 1)));
        if (store != null) store.complete("ORDER_CREATE", idempotencyKey, order.getId().value());
        return order;
    }

    public Order get(UUID id) {
        return repository.findById(new OrderId(id)).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Transactional
    public Order cancel(UUID id) {
        var order = get(id);
        try {
            order.cancel();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        repository.save(order);
        eventPublisher.publish(new EventEnvelope("OrderCancelled", order.getId().toString(), "Order",
            java.util.Map.of("orderId", order.getId().toString(), "status", order.getStatus().name(),
                "aggregateVersion", 2)));
        return order;
    }
}