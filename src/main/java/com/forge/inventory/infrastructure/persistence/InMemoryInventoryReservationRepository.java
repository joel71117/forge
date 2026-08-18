package com.forge.inventory.infrastructure.persistence;

import com.forge.inventory.application.port.InventoryReservationRepository;
import com.forge.inventory.domain.InventoryReservation;
import com.forge.inventory.domain.InventoryReservationId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!local")
public class InMemoryInventoryReservationRepository implements InventoryReservationRepository {
    private final ConcurrentHashMap<InventoryReservationId, InventoryReservation> store = new ConcurrentHashMap<>();

    @Override
    public Optional<InventoryReservation> findById(InventoryReservationId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public InventoryReservation save(InventoryReservation reservation) {
        store.put(reservation.getId(), reservation);
        return reservation;
    }
}