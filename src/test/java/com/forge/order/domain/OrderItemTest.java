package com.forge.order.domain;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Quantity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderItemTest {

    @Test
    void shouldGenerateIdAndUpdateTotals() {
        UUID productId = UUID.randomUUID();

        OrderItem item = new OrderItem(productId, new Quantity(2), Money.of("15.00", Currency.USD));

        assertNotNull(item.getId());
        assertEquals(new Quantity(2), item.getQuantity());
        assertEquals(Money.of("30.00", Currency.USD), item.getSubtotal());
    }
}
