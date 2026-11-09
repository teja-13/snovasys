ALTER TABLE accounts
    ALTER COLUMN currency TYPE VARCHAR(3)
    USING btrim(currency);

ALTER TABLE transfers
    ALTER COLUMN currency TYPE VARCHAR(3)
    USING btrim(currency);
