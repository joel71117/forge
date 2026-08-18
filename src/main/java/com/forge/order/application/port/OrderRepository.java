package com.forge.order.application.port;

import com.forge.order.domain.Order;
import com.forge.order.domain.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(OrderId id);

    Optional<Order> findByIdempotencyKey(String key);

    List<Order> findAll(int page, int size);

    Order save(Order order);
}