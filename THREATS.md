# BalanceVault threat model

Assets are credentials, JWTs, account balances, immutable ledger and audit history, database credentials, and deployment secrets. Trust boundaries are the browser-to-Nginx edge, Nginx-to-backend network, backend-to-PostgreSQL/Redis network, and CI/CD-to-AWS.

| Threat | Impact | Mitigation | Residual risk |
|---|---|---|---|
| Credential stuffing and brute force | Account takeover | BCrypt, login rate limiting, generic authentication errors | Compromised user passwords |
| JWT theft or stale roles | Unauthorized access | Short-lived tokens; every JWT resolves the active user and current roles | Token usable until expiry |
| IDOR and privilege escalation | Unauthorized account or user changes | Ownership checks and server-side role authorization | Application bugs require regression tests |
| Replay and duplicate writes | Duplicate money movement | User-scoped idempotency keys, fingerprints, database uniqueness | Retention policy is operational |
| Concurrent transfers | Double spend | Pessimistic account locks, deterministic lock order, non-negative constraint | Database availability |
| Ledger tampering | Loss of financial evidence | Append-only application paths, constrained journal rows, audit events | Database superuser access |
| Redis outage or bypass | Unlimited sensitive requests | Redis-backed limits fail closed for protected mutations | Availability impact during outage |
| Secret or database exposure | Full compromise | Environment variables, private services, ignored `.env`, no secret logging | Host/IAM compromise |
| Container/deployment compromise | Service takeover | Private backend/database/Redis, health gates, persistent volumes | AWS and image supply-chain risk |
