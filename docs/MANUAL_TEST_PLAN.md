# MARAM Payroll — Manual Test Plan (End-to-End)

> Comprehensive manual testing scenarios to validate the payroll application end-to-end.
> Every scenario lists **preconditions, steps, test data and expected results**.
> Payroll scenarios include **fully worked numeric expectations** computed against the
> real calculation engine (`PayrollCalculationService` + `*Calculator`), so a tester can
> confirm each amount to the cent (TND, 2 decimals, HALF_UP rounding).

- **Backend:** Spring Boot (`payroll-backend`) — REST API, PostgreSQL + Flyway, MinIO (documents), JWT auth.
- **Frontend:** React + MUI (`payroll-frontend`) — i18n EN/FR.
- **Currency:** Tunisian Dinar (TND), all money rounded to **2 decimals, HALF_UP**.
- **Working days basis:** **26** days/month (constant `Money.WORKING_DAYS`).

---

## 0. Test Environment & Global Preconditions

| Item | Value |
|------|-------|
| Default admin (bootstrapped on first start) | username `admin` / password `Admin@123!` |
| Admin email | `admin@maram.local` |
| Seeded salary categories | `CAT1` (Category 1), `CAT3` (Category 3) |
| Seeded department | `GEN` (General) |
| Seeded roles | `ADMIN`, `HR_MANAGER`, `FINANCE_MANAGER`, `MANAGER`, `EMPLOYEE` |
| Seeded tax year | 2026 (IRPP brackets, CNSS 5.95%, HEALTH 0%, family + pro abatements) |
| **Salary scales** | **NOT seeded** — must be created before payroll, or every employee is skipped |
| Health insurance rate | 0.00% (deduction = 0 unless reconfigured) |

> ⚠️ **Critical setup rule:** Payroll calculation **skips** any active employee who is missing
> (a) a `baseSalary`, or (b) a matching `SalaryScale` for their `(category, echelon, year)`.
> Always create the salary scale and set base salary first.

### Recommended baseline setup (used by the payroll worked examples)

1. Log in as `admin`.
2. **Configuration → Salary Scales:** create a scale → Category `CAT1`, Échelon `3`, Year `2026`, Multiplier `4.000`.
3. **Configuration → Allowances:** confirm the seeded defaults exist (created by migration `V11`):

   | Type | Amount (TND) | Attendance-adjusted |
   |------|-------------|---------------------|
   | PRESENCE | 9.036 | ✅ Yes |
   | TRANSPORT | 87.165 | ❌ No (fixed) |
   | DILIGENCE | 16.011 | ❌ No (fixed) |
   | MEAL | 22.500 | ✅ Yes |
   | CHILD | 5.000 | per child |

4. **Payroll Periods:** create period `2026-01` (Month 1, Year 2026), working days 26.

---

## 1. Roles & Permission Matrix (reference for RBAC tests)

| Permission | ADMIN | HR_MANAGER | FINANCE_MANAGER | MANAGER | EMPLOYEE |
|------------|:----:|:----:|:----:|:----:|:----:|
| employee.view | ✅ | ✅ | ✅ | ✅ | — |
| employee.create / edit | ✅ | ✅ | — | — | — |
| employee.delete | ✅ | — | — | — | — |
| employee.import | ✅ | ✅ | — | — | — |
| attendance.view | ✅ | ✅ | — | ✅ | ✅ |
| attendance.record / import | ✅ | ✅ | — | record only | — |
| payroll.view | ✅ | ✅ | ✅ | — | ✅ |
| payroll.calculate | ✅ | ✅ | ✅ | — | — |
| payroll.approve | ✅ | — | ✅ | — | — |
| payroll.export | ✅ | — | ✅ | — | — |
| report.view / export | ✅ | ✅ | ✅ | view only | — |
| config.manage | ✅ | — | — | — | — |
| user.manage / role.manage | ✅ | — | — | — | — |
| performance.rate | ✅ | ✅ | — | ✅ | — |

---

## 2. Authentication & Session

| # | Scenario | Steps | Test data | Expected |
|---|----------|-------|-----------|----------|
| AUTH-01 | Successful login | Open `/login`, enter creds, submit | `admin` / `Admin@123!` | Redirect to Dashboard `/`; user menu shows `admin` + role ADMIN; access token stored |
| AUTH-02 | Login button disabled when empty | Leave username or password blank | — | Submit button disabled |
| AUTH-03 | Wrong password | Submit bad password | `admin` / `wrong` | Error alert "invalid credentials"; no redirect; `failedLoginAttempts` increments server-side |
| AUTH-04 | Redirect to originally requested page | While logged out, open `/employees`; then log in | — | After login, lands on `/employees` (not `/`) |
| AUTH-05 | Protected route while unauthenticated | Clear session, open `/payroll` | — | Redirect to `/login` |
| AUTH-06 | `GET /auth/me` | Call after login | valid token | Returns username, roles, permissions array |
| AUTH-07 | Token refresh | Wait for access token expiry / call `POST /auth/refresh` | valid refresh token | New access + refresh token returned (rotation); old refresh token revoked |
| AUTH-08 | Logout | Click logout | — | Refresh token revoked; redirect to `/login`; protected routes inaccessible |
| AUTH-09 | Reused/revoked refresh token | Call `/auth/refresh` twice with same old token | revoked token | Second call rejected (401) |
| AUTH-10 | Language persists across login | Switch to FR on login page, log in | — | App stays French; `localStorage.maram.lang = fr` |

---

## 3. User Management (ADMIN only — `user.manage`)

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| USR-01 | Create user | username `hrmgr` (3–100 chars), email valid, password 8–100 chars, roles `[HR_MANAGER]` | 201; user created ACTIVE |
| USR-02 | Create user — short password | password `123` | 400 validation error (min 8) |
| USR-03 | Create user — invalid email | `not-an-email` | 400 validation error |
| USR-04 | Create user — duplicate username | existing `admin` | 409/400 conflict |
| USR-05 | Create user — empty roles | roles `[]` | 400 (roles must be non-empty) |
| USR-06 | Update roles | set `hrmgr` → `[FINANCE_MANAGER]` | Roles replaced; permissions change on next login |
| USR-07 | Deactivate user | `POST /users/{id}/deactivate` | Status INACTIVE; user can no longer log in |
| USR-08 | Non-admin blocked | Login as HR_MANAGER, call `GET /users` | 403 Forbidden / Unauthorized page |

---

## 4. Departments & Positions

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| DEP-01 | List departments | — | Shows seeded `GEN`; any created depts |
| DEP-02 | Create department (config.manage) | code `IT`, name `Information Technology`, desc optional | 201; appears in list and in EmployeeForm dropdown |
| DEP-03 | Create — duplicate code | code `GEN` | 400/409 conflict (code unique) |
| DEP-04 | Update department | rename `IT` → `IT & Systems` | Updated |
| DEP-05 | Delete department | empty dept | 204 |
| DEP-06 | Non-config-manager blocked | HR_MANAGER tries POST | 403 |
| POS-01 | Create position | code `DEV`, name `Developer`, departmentId `IT` | 201 |
| POS-02 | List positions filtered by dept | `?department_id=IT` | Only IT positions |

---

## 5. Salary Categories & Salary Scales

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| CAT-01 | List categories (read-only) | — | `CAT1`, `CAT3` |
| SCL-01 | Create salary scale | Category `CAT1`, Échelon `3`, Year `2026`, Multiplier `4.000`, validFrom `2026-01-01` | 201; appears in Configuration → Salary Scales for year 2026 |
| SCL-02 | Échelon out of range | Échelon `0` or `15` | 400 (1–14) |
| SCL-03 | Filter by year | `?year=2026` vs `?year=2025` | Only matching year rows |
| SCL-04 | Update multiplier | change `4.000` → `4.042` | Updated; affects next payroll calc |
| SCL-05 | Delete scale | — | 204; employees in that category/échelon now **skipped** on payroll |

---

## 6. Employee Management

### 6.1 Create / Validate

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| EMP-01 | Create valid employee | id `E001`, first `Ahmed`, last `Ben Ali`, hireDate `2024-03-01`, dept `GEN`, category `CAT1`, échelon `3`, baseSalary `560`, gender `M`, family `M0`, children `0`, email `ahmed@x.tn` | 201; redirect to detail page; appears in list |
| EMP-02 | Missing required field | blank `lastName` | Client + server validation error on field |
| EMP-03 | Future hire date | hireDate `2030-01-01` | 400 `@PastOrPresent` violation |
| EMP-04 | Invalid gender | gender `X` | 400 (pattern `[MH]`) |
| EMP-05 | Échelon out of range | échelon `20` | 400 (1–14) |
| EMP-06 | Invalid email | `bad@@` | 400 / field error |
| EMP-07 | Duplicate employeeId | id `E001` again | 400/409 (unique) |
| EMP-08 | Duplicate CNSS number | reuse a CNSS already on file | 400/409 (unique if provided) |
| EMP-09 | Negative children | children `-1` | 400 (`@Min(0)`) |
| EMP-10 | Base salary must be > 0 (UI) | baseSalary `0` or empty | Form blocks submit |

### 6.2 Read / Update / Delete

| # | Scenario | Expected |
|---|----------|----------|
| EMP-11 | View detail | Identity, Employment, Contact & Payment sections render; Documents section present |
| EMP-12 | Edit employee | PUT succeeds; changed fields persisted; redirect to detail |
| EMP-13 | Set status TERMINATED + terminationDate/reason | Status chip updates; employee excluded from payroll (only `ACTIVE` are processed) |
| EMP-14 | Delete employee (ADMIN) | 204; removed from list |
| EMP-15 | Delete blocked for HR_MANAGER | 403 (no `employee.delete`) |
| EMP-16 | List filters | Search by name/ID, filter by department + status return correct subset; pagination 10/25/50/100 works |

### 6.3 Employee Import (.xlsx)

**Required columns:** `employeeId, firstName, lastName, hireDate (YYYY-MM-DD), departmentCode, categoryCode`
**Optional:** `echelon, gender, email, cnssNumber, nationalId, phoneNumber, baseSalary, familyStatus, numberOfChildren`

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| IMP-E-01 | Preview valid file | 3 rows, all valid, dept `GEN`, cat `CAT1` | Preview shows 3 valid / 0 invalid; Import button enabled |
| IMP-E-02 | Preview with bad rows | row with unknown `departmentCode=ZZZ` | That row flagged ERROR with reason; Import disabled |
| IMP-E-03 | Duplicate id within file | two rows id `E100` | Flagged invalid |
| IMP-E-04 | Atomic commit | one invalid row in set | **Entire import rejected** (all-or-nothing); 400; no rows written |
| IMP-E-05 | Successful import | all valid | Rows written; redirect to `/employees` after ~1.2s; new employees listed |
| IMP-E-06 | Non-importer blocked | MANAGER uploads | 403 |

---

## 7. Attendance

**Status → day fraction & worked-day contribution (basis 26):**

| Status | Fraction | Counts toward days worked? | Import code |
|--------|---------|----------------------------|-------------|
| PRESENT | 1.0 | ✅ 1 | `8` or `7` |
| HALF_DAY | 0.5 | ✅ 0.5 | `4` |
| LEAVE | 1.0 | ✅ 1 (paid) | `C` |
| HOLIDAY | 1.0 | ✅ 1 | `H` |
| ABSENT | 0.0 | ❌ 0 | `A` |
| WEEKEND | 0.0 | ❌ 0 | `W` |

> Days worked are **capped at the period's working days** (26). No attendance captured at all ⇒ engine assumes **full** period worked.

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| ATT-01 | Record single day | Employee `E001`, date, status PRESENT, hours 8 | 201; calendar cell colored "present" |
| ATT-02 | Hours out of range | hoursWorked `20` | 400 (`@DecimalMax 16.0`) |
| ATT-03 | Negative hours | `-1` | 400 (`@DecimalMin 0.0`) |
| ATT-04 | Duplicate (employee, date) | record same day twice | Update existing or unique-constraint error (no duplicate row) |
| ATT-05 | Edit a day | change PRESENT → ABSENT via dialog | Cell recolors; summary recalculates |
| ATT-06 | Delete a day | open existing cell, delete | Record removed; summary updates |
| ATT-07 | Monthly summary | `GET /attendance/employee/E001/summary?year=2026&month=1` | Returns present/half/absent/leave counts + day records + attendance rate |
| ATT-08 | Calendar navigation | switch employee, prev/next month | Grid + summary chips update |
| ATT-09 | Attendance report | `/attendance/report?year=2026&month=1` | Per-employee aggregate rows, worst attendance first |

### 7.1 Attendance Import (.xlsx grid)

Layout: **Col 0** = employeeId; **Cols 1..N** = day numbers of month with status codes (`8/7/4/A/C/H/W`, blank = no record).

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| IMP-A-01 | Preview valid grid | employee `E001` row, days filled `8` except 4 `A` | Preview: recognized days count, status OK |
| IMP-A-02 | Unknown employee row | id `E999` | Row flagged ERROR |
| IMP-A-03 | Duplicate date for employee | same day twice | Invalid |
| IMP-A-04 | Atomic commit | one bad row | Whole import rejected; nothing written |
| IMP-A-05 | Commit valid | — | Records written; reflected in calendar/summary |

---

## 8. Configuration — Tax, Allowances, Periods

### 8.1 Tax Configuration (`config.manage`)

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| TAX-01 | List 2026 IRPP | `?year=2026` | 6 real Tunisian brackets (see Appendix A) + CNSS 5.95 + HEALTH 0 |
| TAX-02 | Add bracket | type IRPP, year 2026, min 0, max 1500, rate 0 | 201 |
| TAX-03 | Negative rate | rate `-5` | 400 (must be ≥ 0) |
| TAX-04 | Change CNSS rate | edit CNSS to `9.18` | Next payroll uses new rate |
| TAX-05 | Delete bracket | — | 204; payroll re-derives brackets from remaining rows |

### 8.2 Allowances (`config.manage`)

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| ALW-01 | List | — | PRESENCE/TRANSPORT/DILIGENCE/MEAL/CHILD seeded |
| ALW-02 | Add allowance | type MEAL, amount 25.000, attendanceAdjusted ✅ | 201; used in next calc (date-effective) |
| ALW-03 | Negative amount | `-10` | 400 |
| ALW-04 | Effective-date filtering | endDate set before period start | Excluded from that period's calc |

### 8.3 Payroll Periods — lifecycle

State machine: **DRAFT ⇄ LOCKED → PROCESSING → FINALIZED → PAID** (PAID terminal).

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| PER-01 | Create period | month 1, year 2026 | 201; code `2026-01`, status DRAFT, working days 26 |
| PER-02 | Invalid month | month 13 | 400 (1–12) |
| PER-03 | Transition DRAFT→LOCKED | — | Status LOCKED |
| PER-04 | Unlock LOCKED→DRAFT | — | Back to DRAFT |
| PER-05 | Calculate moves DRAFT→PROCESSING | run payroll calc on DRAFT period | Period auto-set to PROCESSING |
| PER-06 | Finalize / Pay | PROCESSING→FINALIZED→PAID | Each transition persists timestamps |
| PER-07 | Delete only DRAFT | try delete a PAID/PROCESSING period | Rejected; DRAFT delete → 204 |
| PER-08 | Recalculate PAID period | call calc on PAID | **Rejected** "Cannot recalculate a PAID period" |

---

## 9. PAYROLL CALCULATION — worked examples & expectations ⭐

> This is the core. Each example gives the **exact employee + config data** and the
> **expected payroll record fields to the cent**. All amounts in TND.

### 9.0 The formula (engine order of operations)

```
base        = configuredBase × salaryScaleMultiplier(category, échelon, year)
adjusted    = base × (daysWorked / 26)                       ← prorates absences
allowances  = presence + transport + diligence + meal + child + performance
gross       = adjusted + allowances
cnss        = gross × 5.95%
irpp        = monthlyIRPP(gross, cnss, familyStatus)         ← annual method, below
health      = gross × 0%  = 0
deductions  = irpp + cnss + health
net         = gross − deductions
```

**Monthly IRPP (Tunisian annual method):**
```
annualBase        = (gross − cnss) × 12
proAbatement      = min( 10% × annualBase , 2000 )           ← professional expenses cap 2000/yr
familyAbatement   = lookup by familyStatus (C=0, M0=300, M1=400, M2=500, M3=600, M4=700)
annualNetTaxable  = max(0, annualBase − proAbatement − familyAbatement)
annualTax         = progressive barème(annualNetTaxable)     ← Appendix A brackets
monthlyIRPP       = annualTax / 12
```

**Allowance rules:** PRESENCE & MEAL scale by `daysWorked/26`; TRANSPORT & DILIGENCE are fixed; CHILD = 5.000 × numberOfChildren; performance bonus = base × (5% if rating 8, 2.5% if rating 7, else 0%).

**Rounding:** every money amount rounded to 2 decimals HALF_UP; the attendance ratio is held at 10 decimals internally.

---

### Example A — Full month, single, no children, no performance rating

**Setup**

| Field | Value |
|-------|-------|
| Salary scale | CAT1 / échelon 3 / 2026 / **multiplier 4.000** |
| configuredBase | 560.000 |
| Attendance | full month (26 days, or no records captured) |
| familyStatus | `C` (single) |
| numberOfChildren | 0 |
| Performance rating | none |

**Expected payroll record**

| Field | Expected (TND) |
|-------|---------------:|
| baseSalary (560 × 4.000) | **2240.00** |
| daysWorked | 26 |
| adjustedSalary | 2240.00 |
| presenceAllowance (9.036 × 26/26) | 9.04 |
| transportAllowance (fixed) | 87.17 |
| diligenceAllowance (fixed) | 16.01 |
| mealAllowance (22.5 × 26/26) | 22.50 |
| childAllowance (5 × 0) | 0.00 |
| performanceBonus (no rating) | 0.00 |
| totalAllowances | 134.72 |
| **grossSalary** | **2374.72** |
| cnssContribution (2374.72 × 5.95%) | 141.30 |
| incomeTaxIrpp | 455.44 |
| healthInsurance | 0.00 |
| totalDeductions | 596.74 |
| **netSalary** | **1777.98** |

**IRPP trace:** annualBase = (2374.72 − 141.30) × 12 = **26801.04**; proAbatement = min(2680.10, 2000) = **2000.00**; familyAbatement (C) = 0; annualNetTaxable = **24801.04**; annualTax = 525 + 1000 + 2500 + (4801.04 × 30% = 1440.31) = **5465.31**; monthlyIRPP = 5465.31 ÷ 12 = **455.44**.

---

### Example B — Full month, married M2, 2 children, performance rating 8 (excellent)

**Setup:** same scale & base as A; familyStatus `M2`; numberOfChildren `2`; performance **rating 8** for the period (via performance rating, +5%).

**Expected**

| Field | Expected (TND) |
|-------|---------------:|
| baseSalary | 2240.00 |
| adjustedSalary | 2240.00 |
| presence / transport / diligence / meal | 9.04 / 87.17 / 16.01 / 22.50 |
| childAllowance (5 × 2) | 10.00 |
| performanceBonus (2240 × 5%) | 112.00 |
| totalAllowances | 256.72 |
| **grossSalary** | **2496.72** |
| cnssContribution (× 5.95%) | 148.55 |
| incomeTaxIrpp | 477.37 |
| totalDeductions | 625.92 |
| **netSalary** | **1870.80** |

**IRPP trace:** annualBase = (2496.72 − 148.55) × 12 = **28178.04**; proAbatement = 2000.00; familyAbatement (M2) = **500**; annualNetTaxable = **25678.04**; annualTax = 525 + 1000 + 2500 + (5678.04 × 30% = 1703.41) = **5728.41**; monthly = ÷12 = **477.37**.

---

### Example C — Partial month (4 absences), single, 1 child, performance rating 7 (good)

**Setup:** same scale & base; **22 days worked** (22 PRESENT + 4 ABSENT in a 26-day period); familyStatus `C`; numberOfChildren `1`; performance **rating 7** (+2.5%).

**Expected**

| Field | Expected (TND) |
|-------|---------------:|
| baseSalary | 2240.00 |
| daysWorked | 22 |
| attendanceRate (22/26) | 0.8462 |
| adjustedSalary (2240 × 22/26) | 1895.38 |
| presenceAllowance (9.036 × 22/26) | 7.65 |
| transportAllowance (fixed) | 87.17 |
| diligenceAllowance (fixed) | 16.01 |
| mealAllowance (22.5 × 22/26) | 19.04 |
| childAllowance (5 × 1) | 5.00 |
| performanceBonus (2240 × 2.5%) | 56.00 |
| totalAllowances | 190.87 |
| **grossSalary** | **2086.25** |
| cnssContribution (× 5.95%) | 124.13 |
| incomeTaxIrpp | 374.05 |
| totalDeductions | 498.18 |
| **netSalary** | **1588.07** |

**IRPP trace:** annualBase = (2086.25 − 124.13) × 12 = **23545.44**; proAbatement = 2000.00; familyAbatement (C) = 0; annualNetTaxable = **21545.44**; annualTax = 525 + 1000 + 2500 + (1545.44 × 30% = 463.63) = **4488.63**; monthly = ÷12 = **374.05**.

---

### 9.x Payroll calculation scenarios

| # | Scenario | Precondition | Expected |
|---|----------|--------------|----------|
| PAY-01 | Calculate period (happy path) | Period 2026-01 DRAFT; employee E001 set up as **Example A** | `POST /payroll/calculate?periodId=…` → summary: calculated 1, skipped 0; record matches Example A; **netSalary 1777.98**; period → PROCESSING |
| PAY-02 | Married + children + rating | E001 as **Example B** | netSalary **1870.80**; childAllowance 10.00; performanceBonus 112.00 |
| PAY-03 | Absences prorate salary | E001 as **Example C** (4 ABSENT) | adjustedSalary 1895.38; netSalary **1588.07**; absencePenalty 0.00 (proration already accounts for absence — no double count) |
| PAY-04 | Skip — no base salary | employee with `baseSalary = null` | Skipped; reason "missing base salary or salary scale"; not in payroll list |
| PAY-05 | Skip — no salary scale | échelon/category with no 2026 scale | Skipped with same reason |
| PAY-06 | Only ACTIVE employees | one TERMINATED employee in dept | TERMINATED excluded from run |
| PAY-07 | No attendance captured | employee with zero attendance rows | Treated as **full 26 days** (Example A result) |
| PAY-08 | Days worked capped at 26 | attendance summing > 26 (e.g. holidays+present) | daysWorked capped at 26; no over-proration |
| PAY-09 | Recalculate (idempotent) | run calc twice on same DRAFT/PROCESSING period | Existing payroll rows updated in place (not duplicated) |
| PAY-10 | Recalculate PAID period | period status PAID | Rejected: "Cannot recalculate a PAID period" |
| PAY-11 | Totals roll-up | multiple employees | summary totalGross/totalDeductions/totalNet = sum of rows (rounded) |
| PAY-12 | CNSS rate change effect | set CNSS to 9.18% then recalc Example A | cnss = 2374.72 × 9.18% = 217.99; net recomputed accordingly |
| PAY-13 | IRPP zero floor | configure so annualNetTaxable ≤ 1500 (e.g. very low base / high abatement) | incomeTaxIrpp = 0.00 |
| PAY-14 | Family abatement applied | compare C vs M4 with identical gross | M4 yields lower IRPP (abatement 700 vs 0) |
| PAY-15 | View list & single | `GET /payroll?periodId=…`, `GET /payroll/{id}` | Returns calculated rows / single record with all fields |

---

## 10. Payroll Approval

| # | Scenario | Role | Expected |
|---|----------|------|----------|
| APR-01 | Approve a DRAFT payroll | FINANCE_MANAGER / ADMIN | `POST /payroll/{id}/approve` → paymentStatus APPROVED; approvedBy/approvedAt set; Approve button hidden after |
| APR-02 | Approve blocked | HR_MANAGER (no `payroll.approve`) | 403; Approve button not rendered |
| APR-03 | Approve non-DRAFT | already APPROVED record | No-op / rejected; status unchanged |

---

## 11. Reports & Exports

| # | Scenario | Endpoint | Expected |
|---|----------|----------|----------|
| RPT-01 | Payslip PDF | `GET /reports/payslip/{payrollId}` | PDF downloads; shows employee, period, gross, each allowance, CNSS, IRPP, **net matching the calc** |
| RPT-02 | Payslip content sanity | Example A payslip | Net = 1777.98; CNSS 141.30; IRPP 455.44 |
| RPT-03 | Payroll Excel export | `GET /reports/payroll/export?periodId=…` (`payroll.export`) | `.xlsx` with one row per employee; totals reconcile |
| RPT-04 | IRPP declaration CSV | `GET /reports/tax/irpp?periodId=…` (`report.export`) | `.csv` with per-employee IRPP amounts |
| RPT-05 | CNSS declaration CSV | `GET /reports/tax/cnss?periodId=…` | `.csv` with per-employee CNSS amounts |
| RPT-06 | Attendance Excel | `GET /reports/attendance/export?year=2026&month=1` | `.xlsx` monthly grid |
| RPT-07 | Export blocked | EMPLOYEE / MANAGER calls export | 403; buttons hidden |

---

## 12. Documents (Employee files)

Supported types: PDF, JPEG, PNG, DOC, DOCX. Max size **10 MB**. Stored in MinIO.

| # | Scenario | Test data | Expected |
|---|----------|-----------|----------|
| DOC-01 | Upload document | type CONTRACT, valid PDF < 10MB | 201; appears in employee's Documents table (name, type, size, uploader) |
| DOC-02 | Oversized file | 12 MB PDF | Rejected (size limit) |
| DOC-03 | Unsupported type | `.exe` | Rejected (content type) |
| DOC-04 | Download | click download | File streams with correct filename (Content-Disposition) |
| DOC-05 | Delete | delete row (needs `employee.edit`) | 204; removed from table |
| DOC-06 | Upload blocked | viewer without `employee.edit` | 403; upload button hidden |

---

## 13. Dashboard & Analytics

| # | Scenario | Expected |
|---|----------|----------|
| DSH-01 | HR dashboard | `GET /dashboard/hr` → headcount, active/inactive, new hires this month, headcount-by-department bars, attendance rate, recent hires table |
| DSH-02 | Payroll dashboard | `GET /dashboard/payroll` → latest period status chip; calculated X/Y; approved; pending; net total; completion % bar |
| DSH-03 | No period yet | before any period created | Friendly "no period" message, no crash |
| DSH-04 | Permission gating | EMPLOYEE sees only allowed cards | HR section hidden without `employee.view` |

---

## 14. Internationalization (EN / FR)

| # | Scenario | Expected |
|---|----------|----------|
| I18N-01 | Switch EN → FR | All nav, labels, buttons, status chips translate; no raw keys shown |
| I18N-02 | Persistence | Reload page → language retained (`localStorage.maram.lang`) |
| I18N-03 | Browser default | Fresh browser (FR locale) → defaults to French |
| I18N-04 | Dynamic lists | Month names / weekday headers in calendar localized |
| I18N-05 | Number/currency | Amounts render consistently (TND, 2 decimals) in both languages |

---

## 15. RBAC / Authorization (negative tests)

| # | Scenario | Expected |
|---|----------|----------|
| RBAC-01 | EMPLOYEE opens `/employees` | Redirect to `/unauthorized` (no `employee.view`) |
| RBAC-02 | MANAGER opens `/payroll` | Unauthorized (no `payroll.view`) |
| RBAC-03 | HR_MANAGER tries approve | 403 |
| RBAC-04 | Sidebar filtering | Each role sees only permitted nav items |
| RBAC-05 | Direct API call bypassing UI | Server enforces `@PreAuthorize`; 403 even if UI button hidden |
| RBAC-06 | Config pages | Only ADMIN (`config.manage`) can mutate tax/allowance/scale/department |

---

## 16. Security & Robustness

| # | Scenario | Expected |
|---|----------|----------|
| SEC-01 | JWT required | API call without token | 401 |
| SEC-02 | Tampered token | altered signature | 401 |
| SEC-03 | Expired access token | use after expiry | 401 → client refreshes |
| SEC-04 | Refresh token rotation | old token invalid after refresh | 401 on reuse |
| SEC-05 | Password hashing | inspect DB | `password_hash` is BCrypt, never plaintext |
| SEC-06 | SQL injection attempt | search box `'; DROP TABLE…` | Safely handled (parameterized), no error/leak |
| SEC-07 | File upload safety | malicious filename / type | Rejected or sanitized |
| SEC-08 | Health endpoints open | `GET /system/ping`, `/system/info` | 200 without auth (no sensitive data) |

---

## 17. End-to-End Happy Path (full regression scenario)

> Run as a single ordered flow to validate the system top-to-bottom.

1. **Login** as `admin` (AUTH-01).
2. **Config:** create salary scale CAT1/3/2026 ×4.000 (SCL-01); confirm allowances (ALW-01).
3. **Department:** create `IT` (DEP-02).
4. **Employee:** create `E001` Ahmed Ben Ali, CAT1 échelon 3, base 560, family `M2`, 2 children (EMP-01).
5. **Attendance:** record 26 PRESENT days for the period (or leave empty for full month) (ATT-01).
6. **Performance:** set rating 8 for E001 for the period.
7. **Period:** create `2026-01` DRAFT (PER-01).
8. **Calculate:** `POST /payroll/calculate?periodId=…` → expect **Example B**: gross **2496.72**, CNSS 148.55, IRPP 477.37, **net 1870.80**; period → PROCESSING (PAY-02).
9. **Review:** open `/payroll`, verify table row matches (PAY-15).
10. **Approve:** approve E001 → APPROVED (APR-01).
11. **Reports:** download payslip PDF (net 1870.80), payroll Excel, IRPP & CNSS CSVs (RPT-01/03/04/05).
12. **Lifecycle:** transition period → FINALIZED → PAID (PER-06).
13. **Guard:** attempt recalculation of PAID period → rejected (PAY-10).
14. **Dashboard:** confirm payroll KPIs reflect the run (DSH-02).
15. **Logout** (AUTH-08).

✅ **Pass criteria:** every step succeeds and all monetary values match the expected figures to the cent.

---

## Appendix A — 2026 Tunisian IRPP annual barème (active config, migration V12)

| Annual taxable from (TND) | to (TND) | Marginal rate |
|--------------------------:|---------:|:-------------:|
| 0 | 1,500 | 0% |
| 1,500 | 5,000 | 15% |
| 5,000 | 10,000 | 20% |
| 10,000 | 20,000 | 25% |
| 20,000 | 50,000 | 30% |
| 50,000 | ∞ | 35% |

- **CNSS (employee):** 5.95% of gross.
- **HEALTH:** 0.00%.
- **Professional expenses abatement:** 10% of (gross − cnss) annualized, **capped 2000 TND/yr**.
- **Family abatements (annual):** C = 0, M0 = 300, M1 = 400, M2 = 500, M3 = 600, M4 = 700.

> Verification anchor (unit test `CalculatorTest.irppRealTunisianAnnualBareme`): tax on 6000 = 725.00; tax on 24913.96 = 5499.19.

---

## Appendix B — Quick expected-value lookup (worked examples)

| Example | Gross | CNSS | IRPP | Net |
|---------|------:|-----:|-----:|----:|
| A — full month, single, 0 children, no rating | 2374.72 | 141.30 | 455.44 | **1777.98** |
| B — full month, M2, 2 children, rating 8 | 2496.72 | 148.55 | 477.37 | **1870.80** |
| C — 22 days, single, 1 child, rating 7 | 2086.25 | 124.13 | 374.05 | **1588.07** |

*All examples use salary scale CAT1 / échelon 3 / 2026 multiplier 4.000, configuredBase 560 (→ base 2240.00), and the seeded allowance defaults.*

---

## Appendix C — Status / enum reference

- **Employment status:** ACTIVE, INACTIVE, LEAVE, TERMINATED (only ACTIVE are paid).
- **Attendance status:** PRESENT, HALF_DAY, ABSENT, LEAVE, HOLIDAY, WEEKEND.
- **Payroll period:** DRAFT ⇄ LOCKED → PROCESSING → FINALIZED → PAID.
- **Payroll payment status:** DRAFT → APPROVED → PAID.
- **Family status codes:** C, M0, M1, M2, M3, M4.
- **Allowance types:** PRESENCE, TRANSPORT, DILIGENCE, MEAL, CHILD.
- **Tax types:** IRPP, CNSS, HEALTH (+ ABATEMENT, ABATEMENT_PRO config rows).
</content>
</invoke>
