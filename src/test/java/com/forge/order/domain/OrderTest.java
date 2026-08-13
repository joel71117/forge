package com.forge.order.domain;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Quantity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderTest {

    @Test
    void shouldGenerateIdAndTrackStatusChanges() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        Order order = new Order(customerId, Currency.USD, new IdempotencyKey("idem-123"));
        OrderItem item = new OrderItem(UUID.randomUUID(), new Quantity(2), Money.of("75.50", Currency.USD));

        order.addItem(item);
        order.confirm();
        order.startProcessing();

        assertNotNull(order.getId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }
}
