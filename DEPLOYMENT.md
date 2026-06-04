# Deployment & Go-Live Runbook — MARAM Payroll Platform

## 1. Prerequisites
- Docker + Docker Compose (or: Java 17 + Maven, Node 20 + npm, PostgreSQL 16, Redis 7, MinIO)
- A copy of the production `.env` (derived from `.env.example`) with **all default
  secrets changed** — see §2.

## 2. Secrets to change before any non-local deployment
| Variable | Why |
|---|---|
| `JWT_SECRET` | Signs access tokens — must be a long random value (≥ 32 chars). |
| `BOOTSTRAP_ADMIN_PASSWORD` | Default `Admin@123!` is public; set a strong one (or rotate immediately after first login). |
| `POSTGRES_PASSWORD` | Database credentials. |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | Object storage credentials. |

Run with `SPRING_PROFILES_ACTIVE=prod` in production (hides error details, tightens logging).

## 3. Deploy
```bash
cp .env.example .env      # then edit secrets
docker compose up -d --build
```
Services: frontend `:3000`, API `:8080/api`, MinIO console `:9001`.
Flyway applies migrations **V1–V12** automatically on backend startup; Hibernate
validates the schema (`ddl-auto: validate`). The default admin is created on first boot.

TLS: terminate HTTPS at your ingress/reverse proxy. HSTS is sent but only takes
effect over HTTPS.

## 4. Data migration (from the Excel workbook)
The real workbook (`docs/prime annuel 2025.xlsm`) is **git-ignored** (employee PII).

### 4a. Employee master data
```bash
pip install openpyxl
python tools/migrate_workbook.py \
  --file "docs/prime annuel 2025.xlsm" \
  --base-url http://localhost:8080/api \
  --user admin --password '<admin-password>'
# Preview first with --dry-run --limit 10
```
This imports identity, family situation, children, category, hire date, CNSS/CIN
from `ETAT PERSONNEL` via the validated `POST /employees` API (existing rows are
skipped).

### 4b. Known limitation — base salaries & salary scales
The workbook's computed cells (base salary, échelon multipliers, `PAIE GLOBAL`
totals) are stored as un-recalculated formulas (`#REF!` / `#N/A`) in the saved
file, so they **cannot be read programmatically as-is**. To migrate them:
1. Open the workbook in Excel, let it recalculate, and **Save** (this populates
   cached values), **or** export `ETAT PERSONNEL` / `suivi ech` to a clean `.xlsx`.
2. Enter base salaries per employee (HR) and salary-scale multipliers under
   **Configuration → Salary scales** (or extend the importer to read the
   recalculated cells).
3. `categoryId` currently maps everyone to `CAT1`; confirm the real category map
   with HR and adjust.

## 5. Reconciliation against `PAIE GLOBAL`
The engine implements the documented Tunisian method and is unit-tested to exact
figures (see `CalculatorTest`, `PayrollEngineIntegrationTest`):
- base = configuredBase × scale multiplier; prorated by attendance
- allowances: presence/meal attendance-adjusted, transport/diligence fixed, per-child, performance
- CNSS = gross × rate (seeded 5.95% — **confirm with finance / `MLES CNSS`**)
- IRPP = annual barème on `(gross − cnss)·12 − pro-abatement(10%,cap 2000) − family-abatement`, ÷ 12
- net = gross − (IRPP + CNSS + health)

Procedure once base salaries/scales are loaded:
1. Create the period, import attendance (`pointage`), calculate payroll.
2. Export the period to Excel (**Payroll → Excel**) and diff against the
   recalculated `PAIE GLOBAL` sheet, line by line.
3. Investigate any difference > 1 cent. Likely calibration points: **CNSS rate**,
   whether the **10% professional abatement** applies to this population, and the
   exact **allowance amounts** (seeded in `allowance_config`, V11).

## 6. Backups
- **PostgreSQL**: nightly `pg_dump` (retain 7 years for payroll/tax — legal).
  ```bash
  docker exec payroll-postgres pg_dump -U payroll payroll | gzip > backup_$(date +%F).sql.gz
  ```
- **MinIO**: `mc mirror` the bucket to off-site/cold storage; payslips & tax
  declarations have a 7-year retention requirement.
- Test a restore before go-live.

## 7. Go-live checklist
- [ ] All default secrets changed; `prod` profile active; HTTPS at ingress
- [ ] Migrations V1–V12 applied; schema validated
- [ ] Employee master data imported & spot-checked
- [ ] Base salaries + salary scales entered
- [ ] CNSS rate, IRPP brackets, abatements, allowance amounts confirmed by finance
- [ ] One historical month calculated and **reconciled to `PAIE GLOBAL`** (0 diffs)
- [ ] Roles assigned (HR, finance, managers); admin password rotated
- [ ] Backups scheduled and a restore tested
- [ ] Monitoring on `/api/actuator/health`

## 8. Post-launch
- Daily health checks; weekly reconciliation for the first cycle.
- Parallel-run with Excel for one full payroll cycle before retiring the workbook.
