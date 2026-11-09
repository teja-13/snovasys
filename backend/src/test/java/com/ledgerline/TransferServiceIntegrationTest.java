package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ledgerline.domain.Account;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.LedgerEntry;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.TransferRepository;
import com.ledgerline.transfer.TransferDtos;
import com.ledgerline.transfer.TransferService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Test
    void transferMovesMoneyAndCreatesBalancedLedgerEntries() {
        AppUser senderUser = userRepository.save(
                new AppUser("Sender", "sender-" + System.nanoTime() + "@example.com", "hash"));
        AppUser recipientUser = userRepository.save(
                new AppUser("Recipient", "recipient-" + System.nanoTime() + "@example.com", "hash"));
        Account sender = accountRepository.save(new Account("LL100000000001", senderUser));
        Account recipient = accountRepository.save(new Account("LL100000000002", recipientUser));
        credit(sender, new BigDecimal("100.00"));

        TransferDtos.TransferResponse response = transferService.transfer(
                new TransferDtos.CreateTransferRequest(
                        sender.getAccountNumber(), recipient.getAccountNumber(), new BigDecimal("40.00")),
                "transfer-1", senderUser);

        assertEquals("COMPLETED", response.status().name());
        assertEquals(new BigDecimal("60.00"), accountRepository.findById(sender.getId()).orElseThrow().getBalance());
        assertEquals(new BigDecimal("40.00"),
                accountRepository.findById(recipient.getId()).orElseThrow().getBalance());
        assertEquals(2, ledgerEntryRepository.count());
        assertEquals(1, transferRepository.count());
    }

    @Test
    void retryWithSameIdempotencyKeyDoesNotMoveMoneyTwice() {
        AppUser senderUser = userRepository.save(
                new AppUser("Sender", "sender-" + System.nanoTime() + "@example.com", "hash"));
        AppUser recipientUser = userRepository.save(
                new AppUser("Recipient", "recipient-" + System.nanoTime() + "@example.com", "hash"));
        Account sender = accountRepository.save(new Account("LL200000000001", senderUser));
        Account recipient = accountRepository.save(new Account("LL200000000002", recipientUser));
        credit(sender, new BigDecimal("100.00"));
        TransferDtos.CreateTransferRequest request = new TransferDtos.CreateTransferRequest(
                sender.getAccountNumber(), recipient.getAccountNumber(), new BigDecimal("40.00"));

        transferService.transfer(request, "transfer-2", senderUser);
        transferService.transfer(request, "transfer-2", senderUser);

        assertEquals(new BigDecimal("60.00"), accountRepository.findById(sender.getId()).orElseThrow().getBalance());
        assertEquals(1, transferRepository.count());
        assertEquals(2, ledgerEntryRepository.count());
    }

    @Test
    void insufficientFundsLeaveBothBalancesAndLedgerUnchanged() {
        AppUser senderUser = userRepository.save(
                new AppUser("Sender", "sender-" + System.nanoTime() + "@example.com", "hash"));
        AppUser recipientUser = userRepository.save(
                new AppUser("Recipient", "recipient-" + System.nanoTime() + "@example.com", "hash"));
        Account sender = accountRepository.save(new Account("LL300000000001", senderUser));
        Account recipient = accountRepository.save(new Account("LL300000000002", recipientUser));

        assertThrows(IllegalStateException.class, () -> transferService.transfer(
                new TransferDtos.CreateTransferRequest(
                        sender.getAccountNumber(), recipient.getAccountNumber(), new BigDecimal("1.00")),
                "transfer-3", senderUser));

        assertEquals(BigDecimal.ZERO.setScale(2),
                accountRepository.findById(sender.getId()).orElseThrow().getBalance());
        assertEquals(BigDecimal.ZERO.setScale(2),
                accountRepository.findById(recipient.getId()).orElseThrow().getBalance());
        assertEquals(0, ledgerEntryRepository.count());
        assertEquals(0, transferRepository.count());
    }

    private void credit(Account account, BigDecimal amount) {
        account.credit(amount);
        accountRepository.save(account);
    }
}
