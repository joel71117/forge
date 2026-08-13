package com.forge.payment.domain;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentTest {

    @Test
    void shouldGenerateIdAndAllowStatusAndAttemptUpdates() {
        UUID orderId = UUID.randomUUID();

        Payment payment = new Payment(orderId, Money.of("100.00", Currency.USD), "stripe");

        assertNotNull(payment.getId());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("stripe", payment.getProvider());

        payment.startProcessing();
        payment.succeed();

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertEquals(1, payment.getAttemptCount());
    }
}
