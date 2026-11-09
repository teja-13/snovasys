# LedgerLine DevOps

## Phase 1: continuous integration

The repository currently has two GitHub Actions workflows:

- `backend.yml` runs on pushes to `main` and pull requests targeting `main`.
  It starts PostgreSQL 18, runs `./mvnw --batch-mode verify`, and builds the
  Spring Boot artifact.
- `frontend.yml` runs on pushes to `main` and pull requests targeting `main`.
  It runs `npm ci`, Oxlint, and the Vite production build.

A merge into `main` produces a push event, so it is covered without a second
deployment-specific trigger. Workflow permissions remain read-only by default;
AWS credentials are not required for this phase.

Inspect a failed run from the repository's **Actions** tab. Open the failed
workflow, select the failed job, and expand the step containing the error.
Pull request checks should be fixed in the branch and rerun before merging.

## Local verification

```bash
cd backend
./mvnw --batch-mode verify

cd ../frontend
npm ci
npm run lint
npm run build
```

The backend tests require PostgreSQL. The repository's root `compose.yaml`
provides a local PostgreSQL service:

```bash
docker compose up -d postgres
```

## Docker readiness

Separate production-oriented Dockerfiles are provided for the next phase:

- `backend/Dockerfile` builds the Spring Boot JAR in a Maven build stage and
  runs it on a Java 25 runtime image.
- `frontend/Dockerfile` builds the Vite application and serves it with Nginx.
  Supply `VITE_API_URL` as a Docker build argument for a deployed backend URL.

Images are not built or published by Phase 1 CI. The next phase will add ECR
publishing, AWS OIDC authentication, and K3s deployment.

## Required repository configuration

Phase 1 requires no AWS secrets or credentials. Future AWS deployment should
use GitHub Actions OIDC and an IAM role, not long-lived access keys. Runtime
database credentials and JWT secrets must remain outside the repository.
