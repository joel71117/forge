package com.forge.order.domain;

import java.util.UUID;

public record CustomerId(UUID value) {
    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId cannot be null.");
        }
    }
}
