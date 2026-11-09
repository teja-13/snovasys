CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(24) NOT NULL UNIQUE,
    owner_id UUID NOT NULL REFERENCES users(id),
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_accounts_balance_nonnegative CHECK (balance >= 0),
    CONSTRAINT ck_accounts_currency_inr CHECK (currency = 'INR'),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    reference VARCHAR(36) NOT NULL UNIQUE,
    initiator_id UUID NOT NULL REFERENCES users(id),
    sender_account_id UUID NOT NULL REFERENCES accounts(id),
    recipient_account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(12) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_transfer_user_idempotency UNIQUE (initiator_id, idempotency_key),
    CONSTRAINT ck_transfers_positive_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_distinct_accounts CHECK (sender_account_id <> recipient_account_id),
    CONSTRAINT ck_transfers_currency_inr CHECK (currency = 'INR'),
    CONSTRAINT ck_transfers_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED'))
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL REFERENCES transfers(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(6) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ledger_transfer_entry_type UNIQUE (transfer_id, entry_type),
    CONSTRAINT ck_ledger_positive_amount CHECK (amount > 0),
    CONSTRAINT ck_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    details TEXT NOT NULL
);

INSERT INTO roles (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'CUSTOMER'),
    ('00000000-0000-0000-0000-000000000002', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000003', 'AUDITOR');

CREATE INDEX idx_accounts_owner_id ON accounts(owner_id);
CREATE INDEX idx_transfers_sender_account_id ON transfers(sender_account_id);
CREATE INDEX idx_transfers_recipient_account_id ON transfers(recipient_account_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
