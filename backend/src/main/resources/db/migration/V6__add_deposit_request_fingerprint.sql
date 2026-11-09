CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE deposits
    ADD COLUMN request_fingerprint VARCHAR(64);

UPDATE deposits
SET request_fingerprint = encode(
    digest(
        a.account_number || '|' || amount::text || '|' || source || '|' ||
        COALESCE(description, '') || '|INR',
        'sha256'
    ),
    'hex'
)
FROM accounts a
WHERE deposits.account_id = a.id
  AND request_fingerprint IS NULL;

ALTER TABLE deposits
    ALTER COLUMN request_fingerprint SET NOT NULL;
