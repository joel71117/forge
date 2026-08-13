package com.forge.inventory.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InventoryTest {

    @Test
    void shouldGenerateIdAndSupportQuantityUpdates() {
        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory(productId, 25, 5);

        assertNotNull(inventory.getId());
        assertEquals(productId, inventory.getProductId());
        assertEquals(25, inventory.getAvailableQuantity());

        inventory.reserve(5);
        assertEquals(20, inventory.getAvailableQuantity());
        assertEquals(10, inventory.getReservedQuantity());

        inventory.release(3);
        assertEquals(23, inventory.getAvailableQuantity());
        assertEquals(7, inventory.getReservedQuantity());
    }
}
