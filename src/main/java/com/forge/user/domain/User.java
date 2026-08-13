package com.forge.user.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A user is an entity with identity and a small lifecycle.
 *
 * <p>We keep role and status explicit so the system can represent customer vs operations
 * behavior without leaking authorization decisions into the domain object itself.</p>
 */
public class User {
    private final UserId id;
    private final String email;
    private final String displayName;
    private UserStatus status;
    private UserRole role;

    public User(String email, String displayName, UserRole role, UserStatus status) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be blank.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        }

        this.id = new UserId(UUID.randomUUID());
        this.email = email.trim();
        this.displayName = displayName.trim();
        this.role = role;
        this.status = status;
    }

    public UserId getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public UserRole getRole() {
        return role;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}