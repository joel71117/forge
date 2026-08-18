package com.forge.inventory.infrastructure.persistence;

import com.forge.inventory.application.port.InventoryRepository;
import com.forge.inventory.domain.Inventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class JpaInventoryRepository implements InventoryRepository {
    private final JpaInventorySpringDataRepository repository;

    public JpaInventoryRepository(JpaInventorySpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Inventory> findByProductId(UUID productId) {
        return repository.findByProductIdForUpdate(productId);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return repository.save(inventory);
    }
}