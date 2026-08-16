package com.forge.inventory.api.dto;

import jakarta.validation.constraints.Positive;

public class IncreaseInventoryRequest {
    @Positive
    public long quantity;
}
