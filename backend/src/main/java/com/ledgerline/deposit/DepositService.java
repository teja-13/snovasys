package com.ledgerline.deposit;

import com.ledgerline.domain.Account;
import com.ledgerline.domain.AccountStatus;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.AuditLog;
import com.ledgerline.domain.Deposit;
import com.ledgerline.domain.LedgerEntry;
import com.ledgerline.domain.LedgerEntryType;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.DepositRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private final AccountRepository accountRepository;
    private final DepositRepository depositRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;

    public DepositService(
            AccountRepository accountRepository,
            DepositRepository depositRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.depositRepository = depositRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public DepositDtos.DepositResponse deposit(
            DepositDtos.CreateDepositRequest request, String idempotencyKey, AppUser initiator) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("A valid Idempotency-Key header is required");
        }
        String fingerprint = fingerprint(request);
        var existing = depositRepository.findByInitiatorIdAndIdempotencyKeyForUpdate(
                initiator.getId(), idempotencyKey);
        if (existing.isPresent()) {
            Deposit deposit = existing.get();
            if (!deposit.getRequestFingerprint().equals(fingerprint)) {
                throw new IllegalStateException("Idempotency key was reused with different deposit data");
            }
            return toResponse(deposit);
        }

        Account account = accountRepository.findByAccountNumberForUpdate(request.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (!account.getOwner().getId().equals(initiator.getId())) {
            throw new IllegalStateException("Deposit account is not owned by the authenticated user");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Deposits require an active account");
        }

        Deposit deposit = new Deposit(
                UUID.randomUUID().toString(), initiator, account, request.amount(),
                request.source(), normalizeDescription(request.description()), idempotencyKey, fingerprint);
        account.credit(request.amount());
        Deposit saved = depositRepository.save(deposit);
        ledgerEntryRepository.save(new LedgerEntry(saved, account, LedgerEntryType.CREDIT, request.amount()));
        auditLogRepository.save(new AuditLog(
                initiator, "DEPOSIT_COMPLETED", "DEPOSIT", saved.getReference(),
                "Simulated deposit from " + request.source()));
        return toResponse(saved);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private String fingerprint(DepositDtos.CreateDepositRequest request) {
        String data = request.accountNumber() + "|" + request.amount().toPlainString()
                + "|" + request.source().name() + "|" + String.valueOf(normalizeDescription(request.description()))
                + "|INR";
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private DepositDtos.DepositResponse toResponse(Deposit deposit) {
        return new DepositDtos.DepositResponse(
                deposit.getReference(), deposit.getAccount().getAccountNumber(),
                deposit.getAmount(), deposit.getCurrency(), deposit.getSource(),
                deposit.getDescription(), deposit.getCreatedAt());
    }
}
