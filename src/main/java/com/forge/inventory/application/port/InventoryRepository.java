package com.forge.inventory.application.port;

import com.forge.inventory.domain.Inventory;

import java.util.Optional;

public interface InventoryRepository {
    Optional<Inventory> findByProductId(java.util.UUID productId);

    Inventory save(Inventory inventory);
}
