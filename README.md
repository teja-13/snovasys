# LedgerLine

Secure financial transaction management system with role-based access control,
double-entry bookkeeping, and an auditable transaction history.

## Current status

The backend foundation and initial PostgreSQL schema are implemented. The
registration, login, JWT access-token authentication, rotating refresh-token
sessions, account management, transfer APIs, frontend, and CI are implemented.
AWS deployment is intentionally reserved for a later phase.

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

Administrative endpoints:

- `GET /api/admin/users` lists users for admins.
- `POST /api/admin/users/{email}/roles` assigns `CUSTOMER`, `ADMIN`, or
  `AUDITOR` to an existing user and records an audit event.

The first administrator must be provisioned through a controlled database
operation or deployment bootstrap process; public registration never grants
privileged roles.

## Continuous integration

GitHub Actions runs the backend and frontend checks on pushes to `main` and on
pull requests targeting `main`. Backend CI runs Maven verification against
PostgreSQL and builds the application artifact. Frontend CI installs the locked
npm dependency set, runs Oxlint, and creates the production Vite build.

Equivalent local checks:

```bash
cd backend && ./mvnw --batch-mode verify
cd ../frontend && npm ci && npm run lint && npm run build
```

The repository includes production-oriented Dockerfiles at
`backend/Dockerfile` and `frontend/Dockerfile` for the future ECR/Kubernetes
phase. Phase 1 does not publish images or access AWS.
