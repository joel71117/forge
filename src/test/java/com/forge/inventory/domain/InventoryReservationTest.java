package com.forge.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InventoryReservationTest {

    @Test
    void shouldGenerateIdAndTrackStatus() {
        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservation reservation = new InventoryReservation(orderId, productId, 3, ReservationStatus.PENDING,
                now.plusSeconds(300), now, now);

        assertNotNull(reservation.getId());
        assertEquals(orderId, reservation.getOrderId());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setUpdatedAt(now.plusSeconds(15));

        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
    }
}
