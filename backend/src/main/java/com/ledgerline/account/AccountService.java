package com.ledgerline.account;

import com.ledgerline.domain.Account;
import com.ledgerline.domain.AccountStatus;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.AuditLog;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.AuditLogRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final int MAX_ACCOUNTS_PER_USER = 3;
    private static final String ACCOUNT_PREFIX = "LL";

    private final AccountRepository accountRepository;
    private final AppUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(
            AccountRepository accountRepository,
            AppUserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AccountDtos.AccountResponse createForCustomer(String ownerEmail, AppUser actor) {
        AppUser owner = userRepository.findByEmailIgnoreCaseForUpdate(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Account owner was not found"));
        if (accountRepository.countByOwnerId(owner.getId()) >= MAX_ACCOUNTS_PER_USER) {
            throw new IllegalStateException("Customer cannot own more than three accounts");
        }
        Account account = new Account(nextAccountNumber(), owner);
        Account saved = accountRepository.save(account);
        auditLogRepository.save(new AuditLog(
                actor, "ACCOUNT_CREATED", "ACCOUNT", saved.getAccountNumber(), "Account created"));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountDtos.AccountResponse> findOwnedBy(AppUser owner) {
        return accountRepository.findAllByOwnerIdOrderByCreatedAtAsc(owner.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountDtos.AccountResponse> findAll() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountDtos.AccountResponse changeStatus(
            String accountNumber, AccountStatus requestedStatus, AppUser actor) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (requestedStatus == AccountStatus.CLOSED && account.getBalance().signum() != 0) {
            throw new IllegalStateException("Account must have a zero balance before closure");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Closed accounts cannot be reopened or changed");
        }
        if (requestedStatus == AccountStatus.CLOSED
                || requestedStatus == AccountStatus.FROZEN
                || requestedStatus == AccountStatus.ACTIVE) {
            account.changeStatus(requestedStatus);
        }
        auditLogRepository.save(new AuditLog(
                actor, "ACCOUNT_STATUS_CHANGED", "ACCOUNT", account.getAccountNumber(),
                "Status changed to " + requestedStatus));
        return toResponse(account);
    }

    private String nextAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = ACCOUNT_PREFIX + randomDigits(12);
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique account number");
    }

    private String randomDigits(int length) {
        StringBuilder digits = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            digits.append(secureRandom.nextInt(10));
        }
        return digits.toString();
    }

    private AccountDtos.AccountResponse toResponse(Account account) {
        return new AccountDtos.AccountResponse(
                account.getId(), account.getAccountNumber(), account.getBalance(),
                "INR", account.getStatus(), account.getCreatedAt());
    }
}
