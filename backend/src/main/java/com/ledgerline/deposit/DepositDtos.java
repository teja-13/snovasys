package com.ledgerline.deposit;

import com.ledgerline.domain.DepositSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class DepositDtos {

    private DepositDtos() {
    }

    public record CreateDepositRequest(
            @NotBlank String accountNumber,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull DepositSource source,
            @Size(max = 500) String description) {
    }

    public record DepositResponse(
            String reference,
            String accountNumber,
            BigDecimal amount,
            String currency,
            DepositSource source,
            String description,
            Instant createdAt) {
    }
}
