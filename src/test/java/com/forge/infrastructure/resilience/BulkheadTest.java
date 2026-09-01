package com.forge.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BulkheadTest {
    @Test
    void releasesPermitWhenOperationFails() {
        Bulkhead bulkhead = new Bulkhead(1);

        assertThrows(IllegalArgumentException.class,
                () -> bulkhead.execute(() -> {
                    throw new IllegalArgumentException("downstream");
                }));
        assertEquals("recovered", bulkhead.execute(() -> "recovered"));
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Bulkhead(0));
        assertThrows(IllegalArgumentException.class, () -> new Bulkhead(-1));
    }
}