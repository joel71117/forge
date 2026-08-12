package com.forge.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductTest {

    @Test
    void shouldGenerateIdAndStoreValues() {
        Instant now = Instant.now();

        Product product = new Product("SKU-001", "Laptop", "Gaming laptop", new BigDecimal("1299.99"), "USD",
                ProductStatus.ACTIVE, now, now);

        assertNotNull(product.getId());
        assertEquals("SKU-001", product.getSku());
        assertEquals("Laptop", product.getName());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());

        product.setPrice(new BigDecimal("1199.99"));
        product.setUpdatedAt(now.plusSeconds(10));

        assertEquals(new BigDecimal("1199.99"), product.getPrice());
    }
}
