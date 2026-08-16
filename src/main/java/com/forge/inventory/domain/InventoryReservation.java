package com.forge.inventory.domain;

import com.forge.commerce.common.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Reservation state machine.
 *
 * <p>It uses explicit methods instead of a public setStatus method so callers cannot bypass
 * the valid transitions and put the object into an impossible state.</p>
 */
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false, updatable = false)
    @Convert(converter = QuantityConverter.class)
    private Quantity quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected InventoryReservation() {
    }

    public InventoryReservation(UUID orderId, UUID productId, Quantity quantity, ReservationStatus status, Instant expiresAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId cannot be null.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null.");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Reservation status cannot be null.");
        }
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public InventoryReservationId getId() {
        return new InventoryReservationId(id);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void reserve() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be reserved.");
        }
        this.status = ReservationStatus.RESERVED;
    }

    public void consume() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED reservations can be consumed.");
        }
        this.status = ReservationStatus.CONSUMED;
    }

    public void release() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED reservations can be released.");
        }
        this.status = ReservationStatus.RELEASED;
    }

    public void expire() {
        if (status == ReservationStatus.CONSUMED || status == ReservationStatus.RELEASED || status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("This reservation cannot expire in its current state.");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public void cancel() {
        if (status == ReservationStatus.CONSUMED || status == ReservationStatus.RELEASED) {
            throw new IllegalStateException("Consumed or released reservations cannot be cancelled.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryReservation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
