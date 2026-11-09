package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AccountNumberFormatTest {

    @Test
    void publicAccountNumbersUseLedgerLinePrefixAndTwelveDigits() {
        assertTrue(Pattern.matches("LL\\d{12}", "LL482917365204"));
    }
}
