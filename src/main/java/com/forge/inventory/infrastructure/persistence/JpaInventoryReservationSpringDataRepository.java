package com.forge.inventory.infrastructure.persistence;

import com.forge.inventory.domain.InventoryReservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaInventoryReservationSpringDataRepository extends JpaRepository<InventoryReservation, UUID> {
}