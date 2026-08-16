package com.forge.order.api.dto;

import java.math.BigDecimal;

public record OrderItemResponse(String productId, long quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}