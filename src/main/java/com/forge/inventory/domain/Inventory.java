package com.forge.inventory.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Inventory is a simple aggregate with invariants.
 *
 * <p>Despite being a domain model, this aggregate keeps a close relationship between the
 * quantity state and the invariant that available + reserved must never become negative.</p>
 */
public class Inventory {
    private final InventoryId id;
    private final UUID productId;
    private long availableQuantity;
    private long reservedQuantity;

    public Inventory(UUID productId, long availableQuantity, long reservedQuantity) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null.");
        }
        if (availableQuantity < 0 || reservedQuantity < 0) {
            throw new IllegalArgumentException("Inventory quantities cannot be negative.");
        }
        this.id = new InventoryId(UUID.randomUUID());
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public InventoryId getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public long getAvailableQuantity() {
        return availableQuantity;
    }

    public long getReservedQuantity() {
        return reservedQuantity;
    }

    public void reserve(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive.");
        }
        if (quantity > availableQuantity) {
            throw new IllegalArgumentException("Cannot reserve more than available inventory.");
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void release(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be positive.");
        }
        if (quantity > reservedQuantity) {
            throw new IllegalArgumentException("Cannot release more than reserved inventory.");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void increase(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Increase quantity must be positive.");
        }
        availableQuantity += quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory inventory)) return false;
        return Objects.equals(id, inventory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
