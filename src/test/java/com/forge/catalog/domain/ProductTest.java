package com.forge.catalog.domain;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductTest {

    @Test
    void shouldGenerateIdAndStoreValues() {
        Product product = new Product(new Sku("SKU-001"), "Laptop", "Gaming laptop",
                Money.of("1299.99", Currency.USD), ProductStatus.ACTIVE);

        assertNotNull(product.getId());
        assertEquals(new Sku("SKU-001"), product.getSku());
        assertEquals("Laptop", product.getName());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertEquals(Money.of("1299.99", Currency.USD), product.getPrice());

        product.deactivate();
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }
}
