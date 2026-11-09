package com.ledgerline.account;

import com.ledgerline.domain.AccountStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(@Email @NotBlank String ownerEmail) {
    }

    public record AccountResponse(
            UUID id,
            String accountNumber,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Instant createdAt) {
    }

    public record StatusRequest(@NotNull AccountStatus status) {
    }
}
