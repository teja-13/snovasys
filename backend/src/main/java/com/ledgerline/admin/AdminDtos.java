package com.ledgerline.admin;

import com.ledgerline.domain.RoleName;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record AssignRoleRequest(@NotNull RoleName role) {
    }

    public record UserResponse(
            UUID id,
            String fullName,
            String email,
            Set<RoleName> roles,
            Instant createdAt) {
    }
}
