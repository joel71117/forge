package com.forge.commerce.common;

/**
 * Sku is a tiny value object around a product identifier.
 *
 * <p>We validate the payload at construction time so invalid SKUs cannot sneak into the
 * domain model and later fail in a less obvious place.</p>
 */
public record Sku(String value) {
    public Sku {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank.");
        }
        value = value.trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
