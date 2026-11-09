# LedgerLine

Secure financial transaction management system with role-based access control,
double-entry bookkeeping, and an auditable transaction history.

## Current status

The backend foundation and initial PostgreSQL schema are implemented. The
authentication, account-management, transfer APIs, frontend, CI, and deployment
are not implemented yet.

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
`DB_PASSWORD`. Local defaults match the Compose service.
