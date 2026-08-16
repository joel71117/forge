package com.forge.order.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class OrderItemRequest {
    @NotNull
    public String productId;
    @Positive
    public long quantity;
    @NotNull
    public BigDecimal unitPrice;
}