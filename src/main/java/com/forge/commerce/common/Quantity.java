package com.forge.commerce.common;

/**
 * Quantity is intentionally positive-only.
 *
 * <p>Using a value object instead of raw integers in order and inventory code keeps the
 * invariant close to the business rule: 0 and negative quantities are never valid here.</p>
 */
public record Quantity(long value) {
    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be strictly positive.");
        }
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
