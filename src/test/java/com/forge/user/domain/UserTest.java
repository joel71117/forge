package com.forge.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserTest {

    @Test
    void shouldGenerateIdAndAllowStatusUpdates() {
        User user = new User("alice@example.com", "Alice", UserRole.CUSTOMER, UserStatus.ACTIVE);

        assertNotNull(user.getId());

        user.suspend();

        assertEquals(UserStatus.SUSPENDED, user.getStatus());
    }
}
