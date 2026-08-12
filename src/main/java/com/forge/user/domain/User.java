package com.forge.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class User {
    private final UserId id;
    private String email;
    private String displayName;
    private UserStatus status;
    private UserRole role;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(String email, String displayName, UserStatus status, UserRole role, Instant createdAt, Instant updatedAt) {
        this.id = new UserId(UUID.randomUUID());
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}