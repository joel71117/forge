package com.forge.inventory.infrastructure.persistence;

import com.forge.inventory.application.port.InventoryReservationRepository;
import com.forge.inventory.domain.InventoryReservation;
import com.forge.inventory.domain.InventoryReservationId;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class JpaInventoryReservationRepository implements InventoryReservationRepository {
    private final JpaInventoryReservationSpringDataRepository repository;

    public JpaInventoryReservationRepository(JpaInventoryReservationSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<InventoryReservation> findById(InventoryReservationId id) {
        return repository.findById(id.value());
    }

    @Override
    public InventoryReservation save(InventoryReservation reservation) {
        return repository.save(reservation);
    }
}