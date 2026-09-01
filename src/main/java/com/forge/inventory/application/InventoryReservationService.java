package com.forge.inventory.application;

import com.forge.common.api.ConflictException;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.common.application.EventEnvelope;
import com.forge.common.application.EventPublisher;
import com.forge.commerce.common.Quantity;
import com.forge.inventory.application.port.*;
import com.forge.inventory.domain.*;
import com.forge.inventory.infrastructure.persistence.InMemoryInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
public class InventoryReservationService {
    private static final String RELEASE = "release";
    private static final String CONSUME = "consume";
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;

    public InventoryReservationService(InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository) {
        this(inventoryRepository, reservationRepository, null);
    }

    @Autowired
    public InventoryReservationService(InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository, EventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InventoryReservation reserve(UUID orderId, UUID productId, long quantity) {
        if (inventoryRepository instanceof InMemoryInventoryRepository) {
            synchronized (this) {
                return reserveInternal(orderId, productId, quantity);
            }
        }
        return reserveInternal(orderId, productId, quantity);
    }

    private InventoryReservation reserveInternal(UUID orderId, UUID productId, long quantity) {
        var inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        try {
            inventory.reserve(quantity);
        } catch (IllegalArgumentException ex) {
            throw new ConflictException(ex.getMessage());
        }
        var reservation = new InventoryReservation(orderId, productId, new Quantity(quantity),
                ReservationStatus.PENDING, Instant.now().plus(Duration.ofHours(1)));
        reservation.reserve();
        inventoryRepository.save(inventory);
        var saved = reservationRepository.save(reservation);
        if (eventPublisher != null) {
            eventPublisher.publish(new EventEnvelope("InventoryReserved", saved.getId().toString(), "Inventory",
                java.util.Map.of("reservationId", saved.getId().toString(), "productId", productId.toString(),
                    "quantity", quantity)));
        }
        return saved;
    }

    @Transactional
    public InventoryReservation release(UUID id) {
        return transition(id, RELEASE);
    }

    @Transactional
    public InventoryReservation consume(UUID id) {
        return transition(id, CONSUME);
    }

    private InventoryReservation transition(UUID id, String action) {
        var reservation = reservationRepository.findById(new InventoryReservationId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        try {
            if (action.equals(RELEASE))
                reservation.release();
            else
                reservation.consume();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        var saved = reservationRepository.save(reservation);
        if (eventPublisher != null) {
            eventPublisher.publish(new EventEnvelope(action.equals(RELEASE) ? "InventoryReleased" : "InventoryConsumed",
                saved.getId().toString(), "Inventory", java.util.Map.of("reservationId", saved.getId().toString())));
        }
        return saved;
    }
}