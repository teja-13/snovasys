package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DatabaseMigrationContractTest {

    @Test
    void initialMigrationDefinesFinancialIntegrityConstraints() throws IOException {
        String migration = readMigration();

        assertTrue(migration.contains("CONSTRAINT ck_accounts_balance_nonnegative CHECK (balance >= 0)"));
        assertTrue(migration.contains("CONSTRAINT ck_transfers_positive_amount CHECK (amount > 0)"));
        assertTrue(migration.contains("CONSTRAINT ck_transfers_distinct_accounts"));
        assertTrue(migration.contains("CONSTRAINT uk_transfer_user_idempotency UNIQUE (initiator_id, idempotency_key)"));
        assertTrue(migration.contains("CONSTRAINT uk_ledger_transfer_entry_type UNIQUE (transfer_id, entry_type)"));
        assertTrue(migration.contains("CONSTRAINT ck_accounts_currency_inr CHECK (currency = 'INR')"));
    }

    @Test
    void initialMigrationSeedsAllSupportedRoles() throws IOException {
        String migration = readMigration();

        assertTrue(migration.contains("'CUSTOMER'"));
        assertTrue(migration.contains("'ADMIN'"));
        assertTrue(migration.contains("'AUDITOR'"));
    }

    @Test
    void currencyMigrationUsesVariableLengthCharacterColumns() throws IOException {
        String migration = readCurrencyMigration();

        assertTrue(migration.contains("ALTER COLUMN currency TYPE VARCHAR(3)"));
        assertTrue(migration.contains("USING btrim(currency)"));
    }

    private String readMigration() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V1__create_initial_schema.sql")) {
            if (input == null) {
                throw new IOException("Initial database migration was not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readCurrencyMigration() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V3__use_varchar_for_currency_codes.sql")) {
            if (input == null) {
                throw new IOException("Currency type migration was not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
