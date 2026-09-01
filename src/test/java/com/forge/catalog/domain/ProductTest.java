package com.forge.catalog.domain;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        @Test
        void shouldNormalizeDetailsAndDefaultMissingDescription() {
        Product product = new Product(new Sku(" SKU-001 "), "  Laptop  ", null,
            Money.of("10", Currency.USD), ProductStatus.ACTIVE);

        assertEquals("SKU-001", product.getSku().value());
        assertEquals("Laptop", product.getName());
        assertEquals("", product.getDescription());

        product.updateDetails("  Updated laptop ", null, Money.of("12.50", Currency.USD));

        assertEquals("Updated laptop", product.getName());
        assertEquals("", product.getDescription());
        assertEquals(Money.of("12.50", Currency.USD), product.getPrice());
        }

        @Test
        void shouldRejectInvalidConstructionAndUpdates() {
        assertThrows(IllegalArgumentException.class,
            () -> new Product(null, "Laptop", "", Money.of("10", Currency.USD), ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
            () -> new Product(new Sku("SKU-001"), " ", "", Money.of("10", Currency.USD), ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
            () -> new Product(new Sku("SKU-001"), "Laptop", "", null, ProductStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
            () -> new Product(new Sku("SKU-001"), "Laptop", "", Money.of("10", Currency.USD), null));

        Product product = new Product(new Sku("SKU-001"), "Laptop", "", Money.of("10", Currency.USD),
            ProductStatus.ACTIVE);
        assertThrows(IllegalArgumentException.class,
            () -> product.updateDetails(null, "description", Money.of("10", Currency.USD)));
        assertThrows(IllegalArgumentException.class,
            () -> product.updateDetails("Laptop", "description", null));
        }

        @Test
        void shouldReflectLifecycleInOrderingRule() {
        Product product = new Product(new Sku("SKU-001"), "Laptop", "", Money.of("10", Currency.USD),
            ProductStatus.ACTIVE);

        assertEquals(true, product.canBeOrdered());
        product.deactivate();
        assertEquals(false, product.canBeOrdered());
        product.discontinue();
        assertEquals(ProductStatus.DISCONTINUED, product.getStatus());
        product.activate();
        assertEquals(true, product.canBeOrdered());
        }
}
