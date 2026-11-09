package com.ledgerline.transfer;

import com.ledgerline.domain.TransferStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class TransferDtos {

    private TransferDtos() {
    }

    public record CreateTransferRequest(
            @NotBlank String senderAccountNumber,
            @NotBlank String recipientAccountNumber,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
    }

    public record TransferResponse(
            String reference,
            String senderAccountNumber,
            String recipientAccountNumber,
            BigDecimal amount,
            String currency,
            TransferStatus status,
            Instant createdAt) {
    }
}
