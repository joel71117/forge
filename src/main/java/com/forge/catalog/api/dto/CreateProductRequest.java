package com.forge.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreateProductRequest {
    @NotBlank
    public String sku;

    @NotBlank
    public String name;

    public String description;

    @NotNull
    @Positive
    public BigDecimal amount;

    @NotBlank
    public String currency;
}
