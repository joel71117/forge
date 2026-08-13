package com.forge.inventory.domain;

import com.forge.commerce.common.Quantity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InventoryReservationTest {

    @Test
    void shouldGenerateIdAndTrackStatus() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(300);

        InventoryReservation reservation = new InventoryReservation(orderId, productId,
                new Quantity(3), ReservationStatus.PENDING, expiresAt);

        assertNotNull(reservation.getId());
        assertEquals(orderId, reservation.getOrderId());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        reservation.reserve();
        reservation.consume();

        assertEquals(ReservationStatus.CONSUMED, reservation.getStatus());
    }
}
