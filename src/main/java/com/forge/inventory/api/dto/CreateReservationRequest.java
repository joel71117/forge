package com.forge.inventory.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateReservationRequest {
    @NotNull
    public String orderId;
    @NotNull
    public String productId;
    @Positive
    public long quantity;
}