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
