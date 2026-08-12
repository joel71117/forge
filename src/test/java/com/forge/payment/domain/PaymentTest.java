package com.forge.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentTest {

    @Test
    void shouldGenerateIdAndAllowStatusAndAttemptUpdates() {
        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();

        Payment payment = new Payment(orderId, new BigDecimal("100.00"), "USD", PaymentStatus.PENDING,
                "stripe", "ref-1", "key-1", 0, now, now);

        assertNotNull(payment.getId());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("stripe", payment.getProvider());

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setAttemptCount(1);
        payment.setUpdatedAt(now.plusSeconds(60));

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertEquals(1, payment.getAttemptCount());
    }
}
