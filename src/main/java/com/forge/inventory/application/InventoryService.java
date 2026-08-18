package com.forge.inventory.application;

import com.forge.common.api.ResourceNotFoundException;
import com.forge.inventory.application.port.InventoryRepository;
import com.forge.inventory.domain.Inventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    public Inventory get(UUID productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    }

    @Transactional
    public Inventory increase(UUID productId, long quantity) {
        var inventory = repository.findByProductId(productId).orElseGet(() -> new Inventory(productId, 0, 0));
        inventory.increase(quantity);
        return repository.save(inventory);
    }
}