package com.forge.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {
    @NotNull
    public String customerId;
    @NotNull
    public String currency;
    @NotEmpty
    @Valid
    public List<OrderItemRequest> items;
}