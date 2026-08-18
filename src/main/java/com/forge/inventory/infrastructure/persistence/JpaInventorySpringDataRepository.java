package com.forge.inventory.infrastructure.persistence;

import com.forge.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaInventorySpringDataRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from Inventory inventory where inventory.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);
}