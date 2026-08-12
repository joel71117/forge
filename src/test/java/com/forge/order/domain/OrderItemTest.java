package com.forge.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderItemTest {

    @Test
    void shouldGenerateIdAndUpdateTotals() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItem item = new OrderItem(orderId, productId, 2, new BigDecimal("15.00"), new BigDecimal("30.00"));

        assertNotNull(item.getId());
        assertEquals(2L, item.getQuantity());
        assertEquals(new BigDecimal("30.00"), item.getSubtotal());

        item.setQuantity(3L);
        item.setSubtotal(new BigDecimal("45.00"));

        assertEquals(3L, item.getQuantity());
        assertEquals(new BigDecimal("45.00"), item.getSubtotal());
    }
}
