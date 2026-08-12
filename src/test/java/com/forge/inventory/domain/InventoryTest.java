package com.forge.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InventoryTest {

    @Test
    void shouldGenerateIdAndSupportQuantityUpdates() {
        Instant now = Instant.now();
        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory(productId, 25, 5, 1, now);

        assertNotNull(inventory.getId());
        assertEquals(productId, inventory.getProductId());
        assertEquals(25, inventory.getAvailableQuantity());

        inventory.setAvailableQuantity(20);
        inventory.setVersion(2L);
        inventory.setUpdatedAt(now.plusSeconds(20));

        assertEquals(20, inventory.getAvailableQuantity());
        assertEquals(2L, inventory.getVersion());
    }
}
