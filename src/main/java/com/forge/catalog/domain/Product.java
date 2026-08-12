package com.forge.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Product {
    private final ProductId id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private ProductStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(String sku, String name, String description, BigDecimal price, String currency,
                   ProductStatus status, Instant createdAt, Instant updatedAt) {
        this.id = new ProductId(UUID.randomUUID());
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
