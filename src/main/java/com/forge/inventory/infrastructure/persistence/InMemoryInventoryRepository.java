package com.forge.inventory.infrastructure.persistence;

import com.forge.inventory.application.port.InventoryRepository;
import com.forge.inventory.domain.Inventory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@Profile("!local")
public class InMemoryInventoryRepository implements InventoryRepository {
    private final ConcurrentMap<UUID, Inventory> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Inventory> findByProductId(UUID productId) {
        return Optional.ofNullable(store.get(productId));
    }

    @Override
    public Inventory save(Inventory inventory) {
        store.put(inventory.getProductId(), inventory);
        return inventory;
    }
}
