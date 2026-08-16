package com.forge.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class UpdateProductRequest {
    @NotBlank
    public String name;
    public String description;
    @PositiveOrZero
    public BigDecimal amount;
}