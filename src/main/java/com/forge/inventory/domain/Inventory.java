package com.forge.inventory.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Inventory {
    private final InventoryId id;
    private final UUID productId;
    private long availableQuantity;
    private long reservedQuantity;
    private long version;
    private Instant updatedAt;

    public Inventory(UUID productId, long availableQuantity, long reservedQuantity, long version, Instant updatedAt) {
        this.id = new InventoryId(UUID.randomUUID());
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.version = version;
        this.updatedAt = updatedAt;
    }
}
