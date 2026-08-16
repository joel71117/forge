package com.forge.inventory.api.dto;

import com.forge.inventory.domain.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(String id, String orderId, String productId, long quantity, ReservationStatus status,
                Instant expiresAt) {
}