CREATE TABLE deposits (
    id UUID PRIMARY KEY,
    reference VARCHAR(36) NOT NULL UNIQUE,
    initiator_id UUID NOT NULL REFERENCES users(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    source VARCHAR(32) NOT NULL,
    description VARCHAR(500),
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_deposit_user_idempotency UNIQUE (initiator_id, idempotency_key),
    CONSTRAINT ck_deposits_positive_amount CHECK (amount > 0),
    CONSTRAINT ck_deposits_currency_inr CHECK (currency = 'INR'),
    CONSTRAINT ck_deposits_source CHECK (
        source IN ('SALARY', 'SAVINGS', 'CASH_DEPOSIT', 'EXTERNAL_BANK_TRANSFER', 'GIFT', 'OTHER')
    )
);

ALTER TABLE ledger_entries
    ALTER COLUMN transfer_id DROP NOT NULL;

ALTER TABLE ledger_entries
    ADD COLUMN deposit_id UUID REFERENCES deposits(id);

ALTER TABLE ledger_entries
    ADD CONSTRAINT ck_ledger_entry_transaction
    CHECK ((transfer_id IS NOT NULL AND deposit_id IS NULL)
        OR (transfer_id IS NULL AND deposit_id IS NOT NULL));

CREATE UNIQUE INDEX uk_ledger_deposit_entry_type
    ON ledger_entries (deposit_id, entry_type)
    WHERE deposit_id IS NOT NULL;

CREATE INDEX idx_deposits_account_id ON deposits(account_id);
CREATE INDEX idx_deposits_created_at ON deposits(created_at);
