# LedgerLine AI-Assisted Development Ledger

This file records meaningful AI-assisted development interactions for
LedgerLine. Entries describe what was requested, what was implemented, what
was verified, and what remains uncertain. It is maintained as part of the
project history and must not claim work that was not performed.

## Entry 1 — 2026-11-09 — Initial project direction

- **Developer request:** Build LedgerLine as a secure financial transaction
  management system with user accounts, internal transfers, double-entry
  bookkeeping, role-based permissions, auditability, error handling, testing,
  and professional DevOps practices.
- **AI contribution:** Asked one focused question at a time to establish the
  current project state, official requirements, roles, account workflow, and
  financial rules instead of generating an entire project immediately.
- **Developer decisions:** Greenfield project; proposed stack of Java/Spring
  Boot/Spring Security, React/TypeScript, PostgreSQL, JWT, Maven/npm, Docker,
  GitHub Actions, and AWS.
- **Verification or correction:** The official challenge document was not
  available as a complete verified copy, so the supplied challenge summary was
  treated as provisional rather than as an invented source of truth.
- **Commit reference:** No commit existed at the time of this entry.

## Entry 2 — 2026-11-09 — MVP roles and account rules

- **Developer request:** Define the initial roles and workflow.
- **AI contribution:** Guided clarification of customer, admin, and auditor
  responsibilities; account ownership; account count; currency; and lifecycle
  behavior.
- **Developer decisions:** Customers may own up to three accounts; all accounts
  use INR; customers and admins may create accounts; accounts can be ACTIVE,
  FROZEN, or CLOSED; closed accounts must have zero balance and cannot reopen.
- **Verification or correction:** The transfer matrix was explicitly fixed so
  both accounts must be ACTIVE when processing occurs. Frozen and closed
  accounts retain history without participating in new transfers.
- **Commit reference:** No commit existed at the time of this entry.

## Entry 3 — 2026-11-09 — Financial integrity and idempotency

- **Developer request:** Define the transfer lifecycle and duplicate-request
  behavior.
- **AI contribution:** Helped make the transfer rules explicit without
  implementing transfer logic prematurely.
- **Developer decisions:** Transfers use PENDING, COMPLETED, FAILED, and
  REVERSED statuses. Account balances are stored for fast reads and verified
  against immutable ledger entries. Transfers require atomic debit/credit
  processing, database locking or equivalent concurrency control, and
  per-user idempotency keys.
- **Verification or correction:** The developer specified that idempotency
  keys are unique per authenticated user, request fingerprints must match on
  retry, and mismatched reuse returns HTTP 409 Conflict. Reversals create new
  entries rather than modifying original entries.
- **Commit reference:** No commit existed at the time of this entry.

## Entry 4 — 2026-11-09 — Backend foundation and database design

- **Developer request:** Start with the backend foundation and database design:
  establish Spring Boot, PostgreSQL connection, project structure, and initial
  entities.
- **AI contribution:** Inspected the empty workspace and installed toolchain,
  scaffolded the Maven backend, added PostgreSQL/Flyway/JPA configuration,
  created initial domain entities, added the first migration, and added a
  Docker Compose PostgreSQL service.
- **Implemented entities:** User, Role, Account, Transfer, LedgerEntry, and
  AuditLog, plus enums for roles, account status, transfer status, and ledger
  entry type.
- **Verification or correction:** The first Initializr metadata version,
  Spring Boot 4.0.10, was not a released Maven parent. It was corrected to
  released Spring Boot 4.0.9 after the build reported the resolution failure.
  Compilation then passed. The context test remained blocked because Docker
  was installed but its daemon was not running.
- **Commit reference:** Pending first repository commit.

## Entry 5 — 2026-11-09 — Repository tracking and CI

- **Developer request:** Add a detailed conversation ledger, push the current
  repository to `https://github.com/teja-13/snovasys.git`, continue development,
  include tests, and create a GitHub workflow that tests the application on
  push.
- **AI contribution:** Added this ledger, a deterministic database migration
  contract test, and a GitHub Actions workflow with PostgreSQL 18 service
  health checks. The workflow runs Maven verification on pushes and pull
  requests targeting `main`.
- **Verification or correction:** The existing context test still depends on a
  running PostgreSQL instance; local Docker availability must be restored
  before it can pass locally. The CI workflow supplies that dependency.
- **Commit reference:** Pending first repository commit and push.

## Entry 6 — 2026-11-09 — Authentication and rotating sessions

- **Developer request:** Continue after the repository was pushed and implement
  the next step with JWT authentication, short-lived access tokens, rotating
  refresh tokens, secure password hashing, logout revocation, and backend role
  enforcement.
- **AI contribution:** Added registration, login, refresh, and logout endpoints;
  BCrypt password hashing; SHA-256 hashes for database refresh-token storage;
  seven-day refresh-token rotation; 15-minute JWT access tokens; JWT resource
  server configuration; and CUSTOMER/ADMIN/AUDITOR authority mapping.
- **Developer decisions:** Refresh tokens are stored as hashes and revoked on
  use. New registrations receive only CUSTOMER and cannot self-assign
  privileged roles.
- **Verification or correction:** Compilation initially exposed an incorrect
  Nimbus JWT encoder call and a missing `HexFormat` import; both were corrected.
  Focused password and refresh-token lifecycle tests were added. Full
  PostgreSQL-backed context testing still depends on a running database.
- **Commit reference:** `9df47e8 feat: add JWT authentication and roles` (local;
  push requires GitHub authentication in this environment).

## Entry 7 — 2026-11-09 — Account management design

- **Developer request:** Continue with account management and define the public
  account-number format.
- **Developer decision:** Account numbers use `LL` followed by 12 random
  digits. They are 14-character public identifiers, generated with a
  cryptographically secure generator, protected by a database unique
  constraint, and never reused after closure.
- **AI contribution:** Implemented customer account creation, admin account
  creation on behalf of an existing customer, the three-account limit,
  ownership listing, admin status changes, zero-balance closure validation,
  and audit records for account creation and status changes.
- **Verification or correction:** Added a format contract test. Database-backed
  account service tests remain part of the next validation pass.
- **Commit reference:** Pending account-management commit.

## Entry 8 — 2026-11-09 — CI schema validation fix

- **Developer request:** Fix the GitHub Actions backend failure caused by
  Hibernate finding PostgreSQL `CHAR(3)` for currency while the JPA mappings
  expected `VARCHAR(3)`. Run the complete `./mvnw --batch-mode verify` command
  and do not disable schema validation.
- **AI contribution:** Inspected the entities, Flyway migrations, JPA
  configuration, tests, and Compose setup. Added forward-only migration V3 to
  convert both `accounts.currency` and `transfers.currency` to `VARCHAR(3)`
  using `btrim(currency)`, preserving the logical currency values and existing
  constraints. Added migration contract coverage and corrected the PostgreSQL
  18 Compose volume target.
- **Verification:** PostgreSQL reported both currency columns as
  `character varying(3)`. Flyway applied migrations V1 through V3, Hibernate
  schema validation completed, and `./mvnw --batch-mode verify` passed with 7
  tests and zero failures or errors.
- **Commit reference:** Pending CI schema-fix commit.
