package com.forge.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class Payment {
    private final PaymentId id;
    private final UUID orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String provider;
    private String providerReference;
    private String idempotencyKey;
    private int attemptCount;
    private final Instant createdAt;
    private Instant updatedAt;

    public Payment(UUID orderId, BigDecimal amount, String currency, PaymentStatus status, String provider,
                   String providerReference, String idempotencyKey, int attemptCount,
                   Instant createdAt, Instant updatedAt) {
        this.id = new PaymentId(UUID.randomUUID());
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.provider = provider;
        this.providerReference = providerReference;
        this.idempotencyKey = idempotencyKey;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
