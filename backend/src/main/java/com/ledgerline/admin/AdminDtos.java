package com.ledgerline.admin;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.ledgerline.domain.RoleName;

import jakarta.validation.constraints.NotNull;

public final class AdminDtos {

    private AdminDtos() {
    }
    public record AssignRoleRequest(@NotNull RoleName role) {
    }

    public record CreateAuditorRequest(
            @jakarta.validation.constraints.NotBlank String fullName,
            @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 12) String temporaryPassword) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record UserResponse(
            UUID id,
            String fullName,
            String email,
            Set<RoleName> roles,
            Instant createdAt,
            boolean active) {
    }
}
