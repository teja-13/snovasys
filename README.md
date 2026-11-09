# LedgerLine

Secure financial transaction management system with role-based access control,
double-entry bookkeeping, and an auditable transaction history.

## Current status

The backend foundation and initial PostgreSQL schema are implemented. The
registration, login, JWT access-token authentication, rotating refresh-token
sessions, and initial role authorization foundation are implemented. Account
management, transfer APIs, frontend, and deployment are not implemented yet.

## Backend development

Requirements: Java 25, Docker Compose, and Maven (or the included Maven
wrapper).

Start PostgreSQL from the repository root:

```bash
docker compose up -d postgres
```

Run backend tests from `backend/`:

```bash
./mvnw test
```

The backend uses environment variables `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. Local defaults match the Compose service. `JWT_SECRET` must be a
long random secret outside local development; access tokens expire after 15
minutes and refresh tokens expire after 7 days.

Authentication endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

New registrations receive only the `CUSTOMER` role. Administrative and auditor
roles must be assigned through an authorized administrative workflow.

## Frontend development

Requirements: Node.js 22 and npm.

From `frontend/`:

```bash
npm ci
npm run dev
```

The frontend expects the backend at `http://localhost:8080` by default. Set
`VITE_API_URL` when the backend is hosted elsewhere:

```bash
VITE_API_URL=https://your-backend.example.com npm run build
```

The backend permits `http://localhost:5173` by default. Set
`CORS_ALLOWED_ORIGINS` to the deployed frontend origin in deployed
environments.

Internal transfers:

- `POST /api/transfers`
- Requires a customer JWT and an `Idempotency-Key` header.
- Transfers lock both accounts in deterministic order, update both balances,
  create one debit and one credit ledger entry, and write an audit record in
  one database transaction.
- Reusing an idempotency key with different transfer data returns `409
  Conflict`.

Read-only history and audit endpoints:

- `GET /api/transfers` returns a customer's sent and received transfers.
  Admins and auditors may view the complete transfer history.
- `GET /api/audit-logs` is restricted to admins and auditors.
