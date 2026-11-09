package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ledgerline.domain.Account;
import com.ledgerline.domain.AppUser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountMoneyInvariantTest {

    @Test
    void rejectsFractionsBelowCurrencyScale() {
        Account account = new Account("LL999999999999", new AppUser("Owner", "owner@example.com", "hash"));

        assertThrows(IllegalArgumentException.class, () -> account.credit(new BigDecimal("1.001")));
    }

    @Test
    void rejectsNonPositiveCredits() {
        Account account = new Account("LL999999999998", new AppUser("Owner", "owner2@example.com", "hash"));

        assertThrows(IllegalArgumentException.class, () -> account.credit(BigDecimal.ZERO));
    }
}
