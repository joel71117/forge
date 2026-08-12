package com.forge.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserTest {

    @Test
    void shouldGenerateIdAndAllowStatusUpdates() {
        Instant now = Instant.now();

        User user = new User("alice@example.com", "Alice", UserStatus.ACTIVE, UserRole.CUSTOMER, now, now);

        assertNotNull(user.getId());

        user.setStatus(UserStatus.SUSPENDED);

        assertEquals(UserStatus.SUSPENDED, user.getStatus());
    }
}
