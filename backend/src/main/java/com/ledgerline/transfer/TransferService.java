package com.ledgerline.transfer;

import com.ledgerline.domain.Account;
import com.ledgerline.domain.AccountStatus;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.AuditLog;
import com.ledgerline.domain.LedgerEntry;
import com.ledgerline.domain.LedgerEntryType;
import com.ledgerline.domain.Transfer;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.TransferRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;

    public TransferService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public TransferDtos.TransferResponse transfer(
            TransferDtos.CreateTransferRequest request, String idempotencyKey, AppUser initiator) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("A valid Idempotency-Key header is required");
        }
        String fingerprint = fingerprint(request);
        var existing = transferRepository.findByInitiatorIdAndIdempotencyKeyForUpdate(
                initiator.getId(), idempotencyKey);
        if (existing.isPresent()) {
            Transfer transfer = existing.get();
            if (!transfer.getRequestFingerprint().equals(fingerprint)) {
                throw new IllegalStateException("Idempotency key was reused with different transfer data");
            }
            return toResponse(transfer);
        }

        List<String> accountNumbers = List.of(
                request.senderAccountNumber(), request.recipientAccountNumber()).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        Account firstLocked = accountRepository.findByAccountNumberForUpdate(accountNumbers.get(0))
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        Account secondLocked = accountRepository.findByAccountNumberForUpdate(accountNumbers.get(1))
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        Account sender = firstLocked.getAccountNumber().equals(request.senderAccountNumber())
                ? firstLocked : secondLocked;
        Account recipient = firstLocked.getAccountNumber().equals(request.recipientAccountNumber())
                ? firstLocked : secondLocked;
        validateAccounts(sender, recipient, initiator);

        Transfer transfer = new Transfer(
                UUID.randomUUID().toString(), initiator, sender, recipient,
                request.amount(), idempotencyKey, fingerprint);
        sender.debit(request.amount());
        recipient.credit(request.amount());
        transfer.complete();
        Transfer savedTransfer = transferRepository.save(transfer);
        ledgerEntryRepository.save(new LedgerEntry(
                savedTransfer, sender, LedgerEntryType.DEBIT, request.amount()));
        ledgerEntryRepository.save(new LedgerEntry(
                savedTransfer, recipient, LedgerEntryType.CREDIT, request.amount()));
        auditLogRepository.save(new AuditLog(
                initiator, "TRANSFER_COMPLETED", "TRANSFER", savedTransfer.getReference(),
                "Internal INR transfer completed"));
        return toResponse(savedTransfer);
    }

    private void validateAccounts(Account sender, Account recipient, AppUser initiator) {
        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Sender and recipient accounts must differ");
        }
        if (!sender.getOwner().getId().equals(initiator.getId())) {
            throw new IllegalStateException("Sender account is not owned by the authenticated user");
        }
        if (sender.getStatus() != AccountStatus.ACTIVE || recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Both accounts must be active");
        }
    }

    private String fingerprint(TransferDtos.CreateTransferRequest request) {
        String data = request.senderAccountNumber() + "|" + request.recipientAccountNumber()
                + "|" + request.amount().toPlainString() + "|INR";
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private TransferDtos.TransferResponse toResponse(Transfer transfer) {
        return new TransferDtos.TransferResponse(
                transfer.getReference(),
                transfer.getSenderAccount().getAccountNumber(),
                transfer.getRecipientAccount().getAccountNumber(),
                transfer.getAmount(), transfer.getCurrency(),
                transfer.getStatus(), transfer.getCreatedAt());
    }
}
