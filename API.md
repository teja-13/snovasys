# API reference

All protected endpoints require `Authorization: Bearer <access-token>`. Customers may access their own accounts and financial operations; auditors have read-only reporting access; administrators manage users and roles.

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | Public; always creates CUSTOMER |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public with refresh token |
| POST | `/api/auth/logout` | Authenticated |
| GET/POST | `/api/accounts` | Customer; list/create own accounts |
| GET | `/api/accounts/all` | ADMIN/AUDITOR |
| POST | `/api/deposits` | Customer; requires `Idempotency-Key` |
| POST | `/api/transfers` | Customer; requires `Idempotency-Key` |
| GET | `/api/transfers` | Authenticated; scope depends on role |
| GET | `/api/audit-logs` | ADMIN/AUDITOR |
| GET | `/api/admin/users` | ADMIN |
| POST | `/api/admin/auditors` | ADMIN |
| POST | `/api/admin/users/{email}/roles` | ADMIN |
| POST | `/api/admin/users/{email}/active` | ADMIN |

Financial requests return a JSON resource with reference, amount, currency, status and timestamp. Reusing an idempotency key with a different payload returns `409`; rate exhaustion returns `429`; unavailable Redis returns `503` when rate limiting is enabled. Health probes are unauthenticated at `/actuator/health/liveness` and `/actuator/health/readiness`.
