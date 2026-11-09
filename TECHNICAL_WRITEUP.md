# BalanceVault technical write-up

BalanceVault is a Spring Boot 4, Java 25, PostgreSQL and React/Vite application. Nginx is the only public container entry point. PostgreSQL stores users, accounts, transfers, deposits, ledger entries and audit logs; Flyway owns forward-only schema changes.

Transfers lock both accounts in account-number order, debit the sender, credit the recipient, and persist the transfer, two ledger entries and audit event in one transaction. Deposits use the same transaction boundary and currently record a customer credit with a simulated funding source. Amounts use `BigDecimal`/`NUMERIC(19,2)` and INR-only constraints.

Idempotency keys are scoped to the initiating user and operation. A SHA-256 request fingerprint rejects payload changes with conflict; matching retries return the original persisted result. PostgreSQL uniqueness and row locks prevent duplicate execution. Flyway V7 adds database triggers that reject updates and deletes to ledger and audit rows.

JWTs are short-lived, while the resource-server converter reloads the user and current roles from PostgreSQL, so deactivation and role changes affect existing tokens. Redis rate limits login, registration, deposits and transfers; when enabled, Redis failure returns `503` rather than silently bypassing controls.

Liveness is dependency-free and readiness includes PostgreSQL/Flyway. Compose persists PostgreSQL and Redis data, exposes only Nginx, and waits for healthy dependencies.

Known limitations: isolated integration coverage requires a working Docker daemon; database role bootstrap and compensating reversals require deployment-specific operational work.
