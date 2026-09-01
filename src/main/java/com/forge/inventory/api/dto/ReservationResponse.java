package com.forge.inventory.api.dto;

import com.forge.inventory.domain.InventoryReservation;
import com.forge.inventory.domain.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(String id, String orderId, String productId, long quantity, ReservationStatus status,
        Instant expiresAt) {
    public static ReservationResponse from(InventoryReservation reservation) {
        return new ReservationResponse(reservation.getId().value().toString(), reservation.getOrderId().toString(),
                reservation.getProductId().toString(), reservation.getQuantity().value(), reservation.getStatus(),
                reservation.getExpiresAt());
    }
}