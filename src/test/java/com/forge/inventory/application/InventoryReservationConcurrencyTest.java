package com.forge.inventory.application;

import com.forge.inventory.infrastructure.persistence.InMemoryInventoryRepository;
import com.forge.inventory.infrastructure.persistence.InMemoryInventoryReservationRepository;
import com.forge.inventory.domain.Inventory;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryReservationConcurrencyTest {
    @Test
    void reservationsNeverExceedAvailableQuantity() throws Exception {
        var inventoryRepository = new InMemoryInventoryRepository();
        var productId = UUID.randomUUID();
        inventoryRepository.save(new Inventory(productId, 10, 0));
        var service = new InventoryReservationService(inventoryRepository, new InMemoryInventoryReservationRepository());
        var executor = Executors.newFixedThreadPool(20);
        var futures = new java.util.ArrayList<Future<?>>();
        for (int i = 0; i < 100; i++) {
            futures.add(executor.submit(() -> { try { service.reserve(UUID.randomUUID(), productId, 1); } catch (RuntimeException ignored) { } }));
        }
        for (var future : futures) future.get();
        executor.shutdown();
        var inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertTrue(inventory.getAvailableQuantity() >= 0);
        assertTrue(inventory.getReservedQuantity() <= 10);
    }
}