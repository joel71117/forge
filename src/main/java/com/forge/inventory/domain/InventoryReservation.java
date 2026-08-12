package com.forge.inventory.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class InventoryReservation {
    private final InventoryReservationId id;
    private final UUID orderId;
    private final UUID productId;
    private long quantity;
    private ReservationStatus status;
    private Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public InventoryReservation(UUID orderId, UUID productId, long quantity, ReservationStatus status,
                               Instant expiresAt, Instant createdAt, Instant updatedAt) {
        this.id = new InventoryReservationId(UUID.randomUUID());
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
