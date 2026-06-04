# MARAM Confection — Payroll Platform

Enterprise payroll platform that replaces the legacy Excel-based payroll process for
MARAM Confection (~91 employees, Tunisian labour law: IRPP, CNSS, allowances, annual prime).

This repository contains the **monorepo** for the platform:

```
RH/
├── payroll-backend/      # Java 17 · Spring Boot 3.2 · PostgreSQL · Flyway · MinIO
├── payroll-frontend/     # React 18 · TypeScript · Vite · MUI · React Query
├── docker-compose.yml    # Postgres + Redis + MinIO + backend + frontend
├── .github/workflows/    # CI pipelines (backend + frontend)
└── docs/                 # Reverse-engineering & implementation roadmap (the *.md files)
```

> **Status:** Sprint 1 — Foundation. The infrastructure, architecture skeleton, database
> schema (Flyway V1–V6) and runnable backend/frontend shells are in place. Business logic
> (payroll engine, auth, CRUD) is delivered in subsequent sprints per the roadmap.

---

## Architecture (high level)

```
┌──────────────┐     HTTPS/JSON      ┌─────────────────────────┐
│ React SPA    │ ──────────────────► │ Spring Boot API         │
│ (Vite + MUI) │ ◄────────────────── │ JWT · RBAC · REST       │
└──────────────┘                     └───────────┬─────────────┘
                                                 │
                        ┌────────────────────────┼────────────────────────┐
                        ▼                        ▼                        ▼
                 ┌────────────┐          ┌────────────┐          ┌────────────┐
                 │ PostgreSQL │          │   Redis    │          │   MinIO    │
                 │  (data)    │          │  (cache)   │          │ (objects)  │
                 └────────────┘          └────────────┘          └────────────┘
```

Six bounded contexts: **Employee**, **Attendance**, **Payroll Engine**, **Performance**,
**Reporting**, **Document**. See [`docs/`](docs/) for the full domain model and sprint plan.

---

## Tech stack

| Layer        | Technology                                                        |
|--------------|-------------------------------------------------------------------|
| Backend      | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA         |
| Migrations   | Flyway                                                             |
| Database     | PostgreSQL 16                                                      |
| Cache        | Redis 7                                                            |
| Object store | MinIO (S3-compatible)                                              |
| Frontend     | React 18, TypeScript, Vite, Material UI, React Query, React Router |
| CI/CD        | GitHub Actions                                                     |
| Container    | Docker / Docker Compose                                            |

---

## Quick start (local development)

### Prerequisites
- Docker & Docker Compose
- (For running outside Docker) Java 17 + Maven 3.9, Node 20+ / npm 10+

### 1. Bring up the full stack with Docker

```bash
cp .env.example .env          # adjust secrets if needed
docker compose up -d --build
```

| Service    | URL                                  |
|------------|--------------------------------------|
| Frontend   | http://localhost:3000                |
| Backend    | http://localhost:8080/api            |
| Swagger UI | http://localhost:8080/api/swagger-ui |
| Health     | http://localhost:8080/api/actuator/health |
| MinIO UI   | http://localhost:9001                |
| PostgreSQL | localhost:5432                       |

### 2. Run services individually (without Docker)

Start only the infrastructure:
```bash
docker compose up -d postgres redis minio
```

Backend:
```bash
cd payroll-backend
mvn spring-boot:run            # http://localhost:8080/api
```

Frontend:
```bash
cd payroll-frontend
npm install
npm run dev                    # http://localhost:3000
```

---

## Repository conventions

- **Database** changes are *only* made through new Flyway migrations (`Vn__*.sql`). Never edit an applied migration.
- **Branching**: `feature/*`, `fix/*` → PR → `main`. CI must be green before merge.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:` …).

See [`docs/ROADMAP_INDEX.md`](docs/ROADMAP_INDEX.md) for the complete 10-sprint plan.
