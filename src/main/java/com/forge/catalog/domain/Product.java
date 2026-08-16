package com.forge.catalog.domain;

import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;

import java.util.Objects;
import java.util.UUID;

/**
 * A product is an aggregate root in the catalog boundary.
 *
 * <p>
 * The object keeps its invariants in the constructor: no blank SKU, no negative
 * price,
 * and a valid lifecycle state. This is the essence of domain modelling: invalid
 * states are
 * hard to create, and business decisions live close to the data they affect.
 * </p>
 */
public class Product {
    private final ProductId id;
    private final Sku sku;
    private String name;
    private String description;
    private Money price;
    private ProductStatus status;

    public Product(Sku sku, String name, String description, Money price, ProductStatus status) {
        if (sku == null) {
            throw new IllegalArgumentException("SKU cannot be null.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank.");
        }
        if (price == null) {
            throw new IllegalArgumentException("Product price cannot be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Product status cannot be null.");
        }

        this.id = new ProductId(UUID.randomUUID());
        this.sku = sku;
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.price = price;
        this.status = status;
    }

    public ProductId getId() {
        return id;
    }

    public Sku getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void updateDetails(String name, String description, Money price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank.");
        }
        if (price == null) {
            throw new IllegalArgumentException("Product price cannot be null.");
        }
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.price = price;
    }

    public boolean canBeOrdered() {
        return status == ProductStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Product product))
            return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
