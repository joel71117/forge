package com.forge.order.api.dto;

import com.forge.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(String id, String customerId, OrderStatus status, String currency,
                BigDecimal total, List<OrderItemResponse> items) {
}