package com.forge.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderTest {

    @Test
    void shouldGenerateIdAndTrackStatusChanges() {
        Instant now = Instant.now();
        UUID customerId = UUID.randomUUID();

        Order order = new Order(customerId, OrderStatus.CREATED, "USD",
                new BigDecimal("75.50"), "idem-123", now, now);

        assertNotNull(order.getId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        order.setStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(now.plusSeconds(25));

        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }
}
