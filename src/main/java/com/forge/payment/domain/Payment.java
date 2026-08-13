package com.forge.payment.domain;

import com.forge.commerce.common.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Payment is a stateful domain model.
 *
 * <p>UNKNOWN is intentionally different from FAILED: a timeout can create uncertainty
 * without proving the payment did not succeed. The status machine makes that distinction clear.</p>
 */
public class Payment {
    private final PaymentId id;
    private final UUID orderId;
    private final Money amount;
    private final String provider;
    private PaymentStatus status;
    private int attemptCount;

    public Payment(UUID orderId, Money amount, String provider) {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId cannot be null.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount cannot be null.");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider cannot be blank.");
        }
        this.id = new PaymentId(UUID.randomUUID());
        this.orderId = orderId;
        this.amount = amount;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
        this.attemptCount = 0;
    }

    public PaymentId getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getProvider() {
        return provider;
    }

    public void startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payments can start processing.");
        }
        this.status = PaymentStatus.PROCESSING;
        this.attemptCount++;
    }

    public void succeed() {
        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING payments can succeed.");
        }
        this.status = PaymentStatus.SUCCEEDED;
    }

    public void fail() {
        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING payments can fail.");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void markUnknown() {
        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING payments can be marked UNKNOWN.");
        }
        this.status = PaymentStatus.UNKNOWN;
    }

    public void cancel() {
        if (status == PaymentStatus.SUCCEEDED || status == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("This payment can no longer be cancelled.");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
