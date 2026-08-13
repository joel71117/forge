package com.forge.commerce.common;

/**
 * Idempotency keys are used to show that the same business action was intentionally retried.
 *
 * <p>The value object prevents empty strings so we do not create a "blank" operation that is
 * impossible to reason about later.</p>
 */
public record IdempotencyKey(String value) {
    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or blank.");
        }
        value = value.trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
