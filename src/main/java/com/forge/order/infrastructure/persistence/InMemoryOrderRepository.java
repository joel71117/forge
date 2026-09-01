package com.forge.order.infrastructure.persistence;

import com.forge.order.application.port.OrderRepository;
import com.forge.order.domain.Order;
import com.forge.order.domain.OrderId;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@Profile("!local")
public class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentMap<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String key) {
        return store.values().stream().filter(order -> order.getIdempotencyKey().value().equals(key)).findFirst();
    }

    @Override
    public List<Order> findAll(int page, int size) {
        var values = new ArrayList<>(store.values());
        int from = page * size;
        return from >= values.size() ? List.of()
                : List.copyOf(values.subList(from, Math.min(values.size(), from + size)));
    }

    @Override
    public Order save(Order order) {
        store.put(order.getId(), order);
        return order;
    }
}