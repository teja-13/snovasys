# BalanceVault

Secure financial transaction management system with role-based access control,
double-entry bookkeeping, and an auditable transaction history.

## Live application

BalanceVault is deployed on AWS and is available at:

**http://16.178.222.175/**

## Current status

The backend foundation and initial PostgreSQL schema are implemented. The
registration, login, JWT access-token authentication, rotating refresh-token
sessions, account management, transfer APIs, frontend, and CI are implemented.
The production deployment uses Docker Compose on AWS EC2 with PostgreSQL,
Redis, Spring Boot, and the React frontend behind Nginx.

## Start locally

Requirements: Docker Desktop or Docker Engine with Compose, Java 25, Maven (or
the included Maven wrapper), Node.js 22, and npm.

1. Copy the example environment file and set local development values:

   ```bash
   cp .env.example .env
   ```

   Keep `.env` local and never commit it. The example values are placeholders;
   use strong, unique values for `POSTGRES_PASSWORD` and `JWT_SECRET`.

2. Start the local PostgreSQL, Redis, backend, and frontend services:

   ```bash
   docker compose up -d --build
   ```

3. Open the application at [http://localhost](http://localhost). The frontend
   proxies `/api/` requests to the backend.

4. Verify service health:

   ```bash
   curl http://localhost/actuator/health/liveness
   curl http://localhost/actuator/health/readiness
   ```

To run the backend and frontend outside Compose, start PostgreSQL and Redis
first, then use:

```bash
cd backend
./mvnw spring-boot:run

# In another terminal:
cd frontend
npm ci
npm run dev
```

The Vite development server is available at
[http://localhost:5173](http://localhost:5173). Set `VITE_API_URL` only when
the backend is not available at the configured default.

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
- Idempotency keys are scoped to the authenticated user and fingerprint the
  complete request. Deposits follow the same exact-retry/mismatched-reuse
  contract.
- Amounts must be positive INR values with at most two fractional digits.
  Deterministic account locking prevents opposite-direction deadlocks and
  double-spending.

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

To create the first administrator locally, add
`LEDGERLINE_BOOTSTRAP_ADMIN_FULL_NAME`, `LEDGERLINE_BOOTSTRAP_ADMIN_EMAIL`, and
`LEDGERLINE_BOOTSTRAP_ADMIN_PASSWORD` to the untracked `.env`, then recreate
only the backend with `docker compose up -d backend`. The bootstrap is
one-time: after the first administrator exists, changing these values does not
create another administrator. Log in through `POST /api/auth/login` or the
frontend with that email and password. Once logged in as an admin, create an
auditor from the admin UI or `POST /api/admin/auditors`; the request requires
`Authorization: Bearer <admin-access-token>` and a full name, email, and
temporary password.

## Docker Compose deployment

The root `compose.yaml` runs PostgreSQL 18, the backend, and the frontend on
the same Docker network. PostgreSQL uses the persistent
`ledgerline-postgres-data` volume and is not published to the host. The backend
waits for PostgreSQL's health check before starting.

For local or EC2 deployment, copy `.env.example` to `.env` and set strong
values for `POSTGRES_PASSWORD` and `JWT_SECRET`. The `.env` file is ignored by
Git. Set `CORS_ALLOWED_ORIGINS` and `VITE_API_URL` to match the public
deployment URLs when they differ from localhost, then run:

```bash
docker compose up -d --build
```

Safe unauthenticated probes are available at
`/actuator/health/liveness` and `/actuator/health/readiness`. Responses never
include credentials, connection details, balances, or other financial data.
Readiness includes PostgreSQL and Flyway.

Redis is optional auxiliary infrastructure. Set
`LEDGERLINE_REDIS_ENABLED=true` and `REDIS_URL` to enable its client/health
integration; PostgreSQL remains authoritative for balances, ledger entries,
and audit history. Redis outage must not trigger destructive data operations.

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

Use only forward Flyway migrations for schema changes. Never reset persistent
volumes or use destructive database commands in local or deployed environments.
Migration V7 adds database triggers that reject updates and deletes to ledger
entries and audit logs; corrections must be represented by new compensating
records.

The repository includes production-oriented Dockerfiles at
`backend/Dockerfile` and `frontend/Dockerfile` for the future ECR/Kubernetes
phase. Phase 1 does not publish images or access AWS.
