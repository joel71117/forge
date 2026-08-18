package com.forge.inventory.application.port;

import com.forge.inventory.domain.InventoryReservation;
import com.forge.inventory.domain.InventoryReservationId;
import java.util.Optional;

public interface InventoryReservationRepository {
    Optional<InventoryReservation> findById(InventoryReservationId id);

    InventoryReservation save(InventoryReservation reservation);
}