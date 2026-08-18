package com.forge.inventory.application;

import com.forge.common.api.ConflictException;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.commerce.common.Quantity;
import com.forge.inventory.application.port.*;
import com.forge.inventory.domain.*;
import com.forge.inventory.infrastructure.persistence.InMemoryInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
public class InventoryReservationService {
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryReservationService(InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
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
        return reservationRepository.save(reservation);
    }

    @Transactional
    public InventoryReservation release(UUID id) {
        return transition(id, "release");
    }

    @Transactional
    public InventoryReservation consume(UUID id) {
        return transition(id, "consume");
    }

    private InventoryReservation transition(UUID id, String action) {
        var reservation = reservationRepository.findById(new InventoryReservationId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        try {
            if (action.equals("release"))
                reservation.release();
            else
                reservation.consume();
        } catch (IllegalStateException ex) {
            throw new ConflictException(ex.getMessage());
        }
        return reservationRepository.save(reservation);
    }
}