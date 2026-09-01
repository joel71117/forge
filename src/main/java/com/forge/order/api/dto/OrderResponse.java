package com.forge.order.api.dto;

import com.forge.order.domain.Order;
import com.forge.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(String id, String customerId, OrderStatus status, String currency,
        BigDecimal total, List<OrderItemResponse> items) {
    public static OrderResponse from(Order order) {
        var items = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProductId().toString(), item.getQuantity().value(),
                        item.getUnitPrice().amount(), item.getSubtotal().amount()))
                .toList();
        return new OrderResponse(order.getId().value().toString(), order.getCustomerId().value().toString(),
                order.getStatus(), order.getCurrency().name(), order.calculateTotal().amount(), items);
    }
}