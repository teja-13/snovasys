package com.ledgerline.transfer;

import java.math.BigDecimal;
import java.time.Instant;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionResponse(
            String reference,
            String type,
            String source,
            String description,
            String accountNumber,
            String counterpartyAccountNumber,
            BigDecimal amount,
            String currency,
            String status,
            Instant createdAt) {
    }
}
