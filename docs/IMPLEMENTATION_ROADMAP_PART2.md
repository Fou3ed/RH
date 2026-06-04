---

# 8. Security Design

## 8.1 Role-Based Access Control Matrix

| Role | Employee CRUD | Attendance Entry | Payroll Calculate | Payroll Approve | Reports | Admin |
|------|---|---|---|---|---|---|
| **ADMIN** | ✅ Full | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Full | ✅ Yes |
| **HR_MANAGER** | ✅ View/Edit | ✅ Yes | ❌ | ❌ | ✅ Limited | ❌ |
| **FINANCE_MANAGER** | ✅ View | ❌ | ❌ | ✅ Yes | ✅ Finance Only | ❌ |
| **MANAGER** | ✅ View | ❌ | ❌ | ❌ | ✅ Department | ❌ |
| **EMPLOYEE** | ❌ Own Only | ❌ | ❌ | ❌ | ✅ Own Only | ❌ |

### 8.2 Permission Matrix (Selected)

```
EMPLOYEE_RESOURCE:
  - employee.list           → HR_MANAGER, ADMIN
  - employee.view           → HR_MANAGER, FINANCE_MANAGER, MANAGER, EMPLOYEE (own)
  - employee.create         → HR_MANAGER, ADMIN
  - employee.edit           → HR_MANAGER, ADMIN
  - employee.delete         → ADMIN
  - employee.import         → HR_MANAGER, ADMIN

ATTENDANCE_RESOURCE:
  - attendance.record       → HR_MANAGER, ADMIN, MANAGER
  - attendance.view         → HR_MANAGER, MANAGER (own dept), EMPLOYEE (own)
  - attendance.import       → HR_MANAGER, ADMIN
  - attendance.report       → HR_MANAGER, ADMIN

PAYROLL_RESOURCE:
  - payroll.calculate       → ADMIN, FINANCE_MANAGER
  - payroll.view            → FINANCE_MANAGER, ADMIN, EMPLOYEE (own)
  - payroll.edit            → ADMIN (draft only)
  - payroll.approve         → FINANCE_MANAGER, ADMIN
  - payroll.reject          → FINANCE_MANAGER, ADMIN
  - payroll.export          → FINANCE_MANAGER, ADMIN

PERFORMANCE_RESOURCE:
  - rating.create           → MANAGER, ADMIN
  - rating.view             → ADMIN, MANAGER, EMPLOYEE (own)
  - rating.approve          → HR_MANAGER

REPORTING_RESOURCE:
  - report.payroll          → FINANCE_MANAGER, ADMIN
  - report.attendance       → HR_MANAGER, ADMIN
  - report.tax              → FINANCE_MANAGER, ADMIN
  - report.export           → FINANCE_MANAGER, HR_MANAGER, ADMIN
```

### 8.3 JWT Token Design

```json
{
  "iss": "payroll.maram.tn",
  "sub": "user_id_123",
  "username": "sonia.chafai",
  "email": "sonia@maram.tn",
  "roles": ["HR_MANAGER"],
  "permissions": ["employee.list", "employee.view", "attendance.record", ...],
  "iat": 1672531200,
  "exp": 1672617600,
  "scope": "api"
}
```

**Token Rotation:**
- Access Token: 15 minutes
- Refresh Token: 7 days
- Refresh endpoint: `POST /api/auth/refresh`

### 8.4 Row-Level Security

**Example:** MANAGER can only see attendance for their department
```sql
-- Query filter applied automatically
WHERE attendance.employee_id IN (
  SELECT id FROM employees WHERE department_id = :current_user_department_id
)
```

---

## 9. API Specification (Core Endpoints)

### 9.1 Authentication APIs

```
POST /api/auth/login
  Request:
    {
      "username": "sonia.chafai",
      "password": "password123"
    }
  Response (200):
    {
      "token": "eyJhbGc...",
      "refreshToken": "eyJhbGc...",
      "expiresIn": 900,
      "user": {
        "id": 25,
        "username": "sonia.chafai",
        "email": "sonia@maram.tn",
        "roles": ["HR_MANAGER"],
        "permissions": [...]
      }
    }
  Response (401):
    {"error": "Invalid credentials"}

POST /api/auth/refresh
  Request:
    {"refreshToken": "eyJhbGc..."}
  Response (200):
    {"token": "eyJhbGc...", "expiresIn": 900}

POST /api/auth/logout
  Response (200):
    {"message": "Logged out successfully"}
```

### 9.2 Employee APIs

```
GET /api/employees?page=0&limit=25&department_id=1&search=sonia
  Response (200):
    {
      "data": [
        {
          "id": 25,
          "employeeId": "2157",
          "firstName": "SONIA",
          "lastName": "CHAFAI",
          "email": "sonia@maram.tn",
          "department": "Human Resources",
          "position": "HR Manager",
          "category": "CAT1",
          "echelon": 3,
          "baseSalary": 1500.00,
          "hireDate": "2018-06-15",
          "employmentStatus": "ACTIVE"
        }
      ],
      "total": 1,
      "page": 0,
      "limit": 25
    }

POST /api/employees
  Request:
    {
      "employeeId": "2157",
      "firstName": "SONIA",
      "lastName": "CHAFAI",
      "dateOfBirth": "1990-03-15",
      "gender": "H",
      "hireDate": "2018-06-15",
      "departmentId": 1,
      "positionId": 5,
      "categoryId": 1,
      "echelon": 3,
      "baseSalary": 1500.00,
      "familyStatus": "M2",
      "numberOfChildren": 2,
      "email": "sonia@maram.tn",
      "cnssNumber": "12345678900",
      "paymentMethod": "BANK_TRANSFER",
      "bankAccountNumber": "TN...",
      "bankCode": "BNA"
    }
  Response (201):
    {"id": 25, "employeeId": "2157", ...}

PUT /api/employees/{id}
  Request: {...updated fields...}
  Response (200): {...updated employee...}

DELETE /api/employees/{id}
  Response (204): No content
```

### 9.3 Payroll APIs

```
POST /api/payroll/periods
  Request:
    {
      "periodMonth": 3,
      "periodYear": 2026,
      "startDate": "2026-03-01",
      "endDate": "2026-03-31",
      "workingDays": 26
    }
  Response (201):
    {
      "id": 1,
      "periodCode": "MARS_2026",
      "status": "DRAFT",
      "employeeCount": 91,
      "totalPayroll": 0.00
    }

POST /api/payroll/calculate
  Request:
    {
      "payrollPeriodId": 1
    }
  Response (202):
    {
      "jobId": "job-123",
      "status": "PROCESSING",
      "message": "Payroll calculation started (async)"
    }

GET /api/payroll?periodId=1&status=DRAFT&page=0&limit=25
  Response (200):
    {
      "data": [
        {
          "id": 100,
          "employeeId": 25,
          "employeeName": "SONIA CHAFAI",
          "daysWorked": 22.5,
          "baseSalary": 560.05,
          "grossSalary": 585.33,
          "allowances": {
            "presence": 7.82,
            "transport": 87.17,
            "diligence": 16.01,
            "meal": 22.50,
            "child": 20.00,
            "performance": 28.00
          },
          "totalAllowances": 181.50,
          "deductions": {
            "absencePenalty": 0.00,
            "irpp": 62.72,
            "cnss": 33.24,
            "healthInsurance": 0.00
          },
          "totalDeductions": 95.96,
          "netSalary": 670.87,
          "status": "DRAFT"
        }
      ],
      "total": 91,
      "totalGross": 50000.00,
      "totalNet": 45000.00
    }

GET /api/payroll/{id}
  Response (200):
    {
      "id": 100,
      "employeeId": 25,
      "payrollPeriodId": 1,
      ... (complete payroll detail)
    }

POST /api/payroll/{id}/approve
  Request:
    {"notes": "Approved for payment"}
  Response (200):
    {"status": "APPROVED", "approvedBy": "finance@maram.tn", "approvedAt": "2026-03-29T10:30:00Z"}

POST /api/payroll/generate-payslips
  Request:
    {"payrollPeriodId": 1}
  Response (200):
    {
      "jobId": "job-456",
      "status": "PROCESSING",
      "message": "Generating 91 payslips..."
    }

GET /api/payroll/export?periodId=1&format=excel
  Response: (File download - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
```

### 9.4 Attendance APIs

```
POST /api/attendance
  Request:
    {
      "employeeId": 25,
      "attendanceDate": "2026-03-15",
      "attendanceStatus": "PRESENT",
      "attendanceCode": "8",
      "daysF fraction": 1.0,
      "clockInTime": "08:00",
      "clockOutTime": "17:00",
      "hoursWorked": 8.0
    }
  Response (201):
    {"id": 1000, ...}

GET /api/attendance?employeeId=25&startDate=2026-03-01&endDate=2026-03-31
  Response (200):
    {
      "data": [
        {
          "id": 1000,
          "date": "2026-03-01",
          "status": "PRESENT",
          "daysWorked": 1.0,
          "code": "8"
        },
        ...
      ]
    }

POST /api/attendance/import
  Request: (multipart/form-data)
    file: attendance_march_2026.xlsx
    periodId: 1
  Response (200):
    {
      "importedCount": 91,
      "skippedCount": 0,
      "errors": []
    }

GET /api/attendance/summary?periodId=1
  Response (200):
    {
      "data": [
        {
          "employeeId": 25,
          "employeeName": "SONIA CHAFAI",
          "daysWorked": 22.5,
          "absenceDays": 3.5,
          "halfDays": 0,
          "attendanceRate": 0.866
        }
      ]
    }
```

### 9.5 Performance Rating APIs

```
POST /api/performance-ratings
  Request:
    {
      "employeeId": 25,
      "payrollPeriodId": 1,
      "ratingScore": 8,
      "ratingCategory": "EXCELLENT",
      "comments": "Outstanding performance in all areas"
    }
  Response (201):
    {"id": 50, ...}

GET /api/performance-ratings?periodId=1&ratingScore=8
  Response (200):
    {
      "data": [
        {
          "id": 50,
          "employeeName": "SONIA CHAFAI",
          "rating": 8,
          "percentage": 5,
          "bonusAmount": 28.00
        }
      ]
    }
```

---

## 10. Sprint Planning (10 Sprints)

### Sprint 1: Project Foundation & Setup (Week 1-2)

**Sprint Goal:** Establish development environment, CI/CD pipeline, and core infrastructure.

**User Stories:**

```
US-001: As a DevOps engineer, I need Docker Compose setup so that 
        the entire stack (PostgreSQL, MinIO, Spring Boot, React) 
        runs locally with one command.
  
  Tasks:
    - Create Dockerfile for Spring Boot backend
    - Create Dockerfile for React frontend
    - Create docker-compose.yml with all services
    - Configure environment variables
    - Set up volume mounts for persistence
    - Document setup instructions
  
  Acceptance Criteria:
    - docker-compose up starts all services
    - Backend API accessible at http://localhost:8080
    - React app accessible at http://localhost:3000
    - PostgreSQL data persists between restarts
    - MinIO console accessible at http://localhost:9000

US-002: As a developer, I need Flyway migrations so that database 
        schema is version-controlled and can be deployed reliably.
  
  Tasks:
    - Create V1__Create_Base_Schema.sql (departments, positions, employees)
    - Create V2__Create_Salary_Tables.sql (salary scales, allowances)
    - Create V3__Create_Attendance_Tables.sql (attendance, payroll periods)
    - Create V4__Create_Payroll_Tables.sql (payroll, components, tax config)
    - Create V5__Create_Audit_Tables.sql (audit logs, users, roles)
    - Test migrations on clean database
  
  Acceptance Criteria:
    - All migrations execute without errors
    - Schema created exactly as specified
    - Indexes created for performance
    - Foreign keys enforced
    - Migrations are reversible (if using Flyway)

US-003: As a frontend developer, I need React + Vite + TypeScript 
        scaffolding so that I can begin building UI components.
  
  Tasks:
    - Initialize Vite project
    - Configure TypeScript with strict mode
    - Install core dependencies (React Query, React Hook Form, MUI)
    - Set up environment files (.env.dev, .env.prod)
    - Create basic project structure
    - Set up ESLint and Prettier
  
  Acceptance Criteria:
    - npm run dev starts development server on port 3000
    - npm run build creates production build
    - TypeScript compiles without errors
    - Linting passes all files
    - Code formatting consistent

US-004: As a backend developer, I need Spring Boot skeleton so that 
        I can begin implementing business logic.
  
  Tasks:
    - Create Maven project structure
    - Configure Spring Boot with properties
    - Set up Spring Data JPA with PostgreSQL
    - Configure Spring Security (JWT support)
    - Create global exception handler
    - Set up Swagger/OpenAPI documentation
  
  Acceptance Criteria:
    - Spring Boot application starts
    - Database connection verified
    - Swagger UI accessible at /swagger-ui.html
    - Health check endpoint responds
    - Global exception handler returns proper error formats

US-005: As a DevOps engineer, I need GitHub Actions CI/CD so that 
        code changes are automatically tested and built.
  
  Tasks:
    - Create .github/workflows/backend-test.yml
    - Create .github/workflows/frontend-test.yml
    - Configure Maven for automated testing
    - Configure npm for automated testing
    - Create build artifact upload
    - Set up code coverage reporting
  
  Acceptance Criteria:
    - GitHub Actions runs on every push
    - All tests execute
    - Build succeeds for main branch
    - Build artifacts available
    - Code coverage reports generated

**Technical Tasks:**
- [ ] Set up Git repositories (frontend, backend)
- [ ] Create docker-compose.yml with PostgreSQL, MinIO
- [ ] Create all Flyway migration files
- [ ] Initialize React + TypeScript project
- [ ] Initialize Spring Boot project
- [ ] Set up GitHub Actions workflows

**Deliverables:**
- Docker Compose file (ready to run)
- Database migrations (V1-V5)
- React skeleton project
- Spring Boot skeleton application
- CI/CD pipelines (GitHub Actions)

**Risks:**
- Docker volume permission issues on Windows → use named volumes
- Flyway SQL syntax variations → test on PostgreSQL 15+
- React build configuration complexity → use Vite defaults

---

### Sprint 2: Authentication & Authorization (Week 3-4)

**Sprint Goal:** Implement JWT-based authentication and role-based access control.

**User Stories:**

```
US-006: As an HR manager, I need login functionality so that I can 
        authenticate and access the payroll system securely.
  
  Tasks:
    - Implement AuthController with /login endpoint
    - Create User and Role entities
    - Implement UserDetailsService
    - Create JwtTokenProvider with token generation
    - Implement JWT token validation filter
    - Create /refresh-token endpoint
  
  Acceptance Criteria:
    - Login with valid credentials returns JWT token
    - Login with invalid credentials returns 401
    - Token expires after 15 minutes
    - Refresh token extends session
    - Logout invalidates token

US-007: As an admin, I need role-based access control so that 
        different users can only access appropriate features.
  
  Tasks:
    - Create Role and Permission entities
    - Implement RBACChecker service
    - Create authorization annotations
    - Implement endpoint-level authorization
    - Create role assignment endpoints
    - Set up permission caching
  
  Acceptance Criteria:
    - Unauthenticated requests return 401
    - Insufficient permissions return 403
    - Admin can access all endpoints
    - HR Manager cannot access finance endpoints
    - Employees can only view own data

US-008: As a frontend developer, I need login form so that users 
        can authenticate to the system.
  
  Tasks:
    - Create Login page component
    - Implement LoginForm using React Hook Form
    - Integrate authentication service
    - Store JWT token in localStorage/sessionStorage
    - Create AuthContext for app-wide authentication
    - Create ProtectedRoute component
  
  Acceptance Criteria:
    - Login form displays username and password fields
    - Form validates input before submission
    - Successful login redirects to dashboard
    - Failed login shows error message
    - Token persists across page refreshes
    - Protected routes block unauthenticated users

US-009: As an admin, I need user management so that I can create 
        users and assign roles.
  
  Tasks:
    - Create UserController with CRUD endpoints
    - Implement UserService
    - Create User management UI
    - Create role assignment UI
    - Implement password reset functionality
    - Create audit trail for user changes
  
  Acceptance Criteria:
    - Admin can create user with email and role
    - Admin can edit user roles
    - Admin can deactivate user accounts
    - Password hashing is bcrypt
    - All user changes are audited

**Deliverables:**
- JWT authentication implementation
- Role-based access control system
- Login page and forms
- User management UI
- Authentication tests

**Risks:**
- Token expiration edge cases → implement refresh token rotation
- Session hijacking → use HTTPS only in production
- Password storage issues → use bcrypt with salt

---

### Sprint 3: Employee Management (Week 5-7)

**Sprint Goal:** Implement complete employee master data management with import capabilities.

**User Stories:**

```
US-010: As an HR manager, I need to create and edit employee records 
        so that I maintain accurate employee information.
  
  Tasks:
    - Implement EmployeeController (CRUD)
    - Implement EmployeeService
    - Create EmployeeMapper (MapStruct)
    - Create Employee form UI (create/edit)
    - Implement employee validation (CNSS format, email, etc.)
    - Create employee profile page
  
  Acceptance Criteria:
    - Can create new employee record
    - Can edit existing employee data
    - CNSS number validated (format + uniqueness)
    - Email format validated
    - Hire date cannot be in future
    - All changes audited

US-011: As an HR manager, I need to import employees from Excel so 
        that I can bulk upload employee data.
  
  Tasks:
    - Implement EmployeeImportService
    - Use Apache POI to parse Excel files
    - Create data validation for import
    - Implement error reporting (line-by-line)
    - Create preview before commit
    - Handle duplicate detection
  
  Acceptance Criteria:
    - Can upload .xlsx file with employee data
    - Excel columns mapped to database fields
    - Validation errors reported per row
    - Preview shows 10-20 rows before commit
    - Duplicate employees detected
    - Import completes atomically (all or nothing)

US-012: As an HR manager, I need to view employee list with filters 
        so that I can find specific employees quickly.
  
  Tasks:
    - Create Employee List page
    - Implement DataGrid with pagination
    - Add filters (department, status, search)
    - Add sorting by any column
    - Create quick action buttons (edit, view, deactivate)
    - Implement bulk actions
  
  Acceptance Criteria:
    - List displays 25 employees per page
    - Can filter by department, status, search term
    - Pagination works correctly
    - Can click row to view details
    - Quick action buttons functional
    - Bulk select checkbox included

US-013: As an HR manager, I need to manage departments and positions 
        so that organizational structure is maintained.
  
  Tasks:
    - Implement DepartmentController (CRUD)
    - Implement PositionController (CRUD)
    - Create Department/Position forms
    - Create hierarchical department view
    - Link employees to departments/positions
    - Create department manager assignment
  
  Acceptance Criteria:
    - Can create/edit departments
    - Can create positions linked to departments
    - Department hierarchy displayed correctly
    - Manager assignment prevents circular references
    - All changes audited

US-014: As an HR manager, I need to manage employee documents so 
        that I can store contracts and certifications.
  
  Tasks:
    - Implement DocumentService (upload/download)
    - Integrate MinIO file storage
    - Create document upload form
    - Create document list on employee profile
    - Implement file deletion with audit
    - Create virus scan before upload
  
  Acceptance Criteria:
    - Can upload files (PDF, DOCX, JPG)
    - Max file size 10MB enforced
    - File stored in MinIO with correct naming
    - Can download files
    - Can delete files with audit trail
    - File access logged

**Deliverables:**
- Complete employee CRUD API
- Employee import service with validation
- Employee management UI (list, detail, form)
- Department/position management
- Document upload/management
- Comprehensive employee tests

**Risks:**
- Large Excel file import timeout → implement chunking/pagination
- Duplicate employee detection complexity → use unique constraints
- File upload security → virus scan + file type validation

---

### Sprint 4: Attendance Management (Week 8-9)

**Sprint Goal:** Implement daily attendance tracking with import and analytics.

**User Stories:**

```
US-015: As an HR manager, I need to record daily attendance so that 
        I can track employee presence.
  
  Tasks:
    - Implement AttendanceController
    - Implement AttendanceService
    - Create Attendance form (date, status, hours)
    - Create attendance validation
    - Implement audit trail
    - Create attendance edit history
  
  Acceptance Criteria:
    - Can record attendance for any date
    - Status options: PRESENT, ABSENT, HALF_DAY, LEAVE
    - Hours worked validated (0-16)
    - Cannot record future attendance
    - Cannot create duplicate attendance (same employee + date)
    - All changes audited

US-016: As an HR manager, I need to import attendance from Excel so 
        that I can bulk upload daily attendance.
  
  Tasks:
    - Implement AttendanceImportService
    - Parse Excel with date columns (1-31)
    - Map cell values to attendance codes (8, 4, A, etc.)
    - Validate entire month before commit
    - Create error report
    - Implement rollback on validation failure
  
  Acceptance Criteria:
    - Can upload .xlsx with 31 date columns
    - Attendance codes (8, 7, 4, A) recognized
    - Validation prevents duplicates
    - Error report shows problematic rows
    - Preview before commit
    - Atomicity guaranteed

US-017: As an HR manager, I need to view attendance in calendar view 
        so that I can visualize presence patterns.
  
  Tasks:
    - Create AttendanceCalendar component
    - Display month calendar with color-coded cells
    - Color coding: Green=Present, Red=Absent, Yellow=Half-day
    - Click cell to edit attendance
    - Show attendance summary (days worked, rate)
    - Export to PDF
  
  Acceptance Criteria:
    - Calendar displays current month
    - Can navigate to previous/next months
    - Cell colors correct for status
    - Click opens edit dialog
    - Summary shows correct totals
    - PDF export works

US-018: As an HR manager, I need attendance reports so that I can 
        analyze patterns and identify issues.
  
  Tasks:
    - Implement AttendanceReportService
    - Create attendance summary report (by employee, dept, period)
    - Implement analytics (attendance rate, absences, half-days)
    - Create top absentees list
    - Create monthly trends chart
    - Export to Excel/PDF
  
  Acceptance Criteria:
    - Report shows attendance rate by employee
    - Can filter by department, date range
    - Top absentees identified
    - Trends chart displays correctly
    - Excel export includes all data
    - PDF export formatted nicely

**Deliverables:**
- Attendance CRUD API
- Attendance import service
- Attendance calendar UI
- Attendance analytics & reports
- Attendance tests

**Risks:**
- Calendar rendering performance (31 days × 91 employees) → use virtualization
- Excel import complexity → validate format strictly
- Timezone issues → store all times in UTC

---

### Sprint 5: Payroll Configuration (Week 10-11)

**Sprint Goal:** Set up payroll infrastructure, salary scales, tax configuration.

**User Stories:**

```
US-019: As a payroll admin, I need to configure salary scales so that 
        base salaries are calculated correctly.
  
  Tasks:
    - Implement SalaryScaleService (CRUD)
    - Create SalaryScale entities (category, échelon, year, multiplier)
    - Create SalaryScale UI table
    - Implement in-place editing
    - Add validation (multiplier must increase year-over-year)
    - Track historical salary scales
  
  Acceptance Criteria:
    - Can create salary scale entries
    - Can edit multipliers for any échelon
    - Year-over-year increases validated
    - Salary scales versioned (historical data preserved)
    - Can view previous years' scales
    - Table includes calculated $ amounts

US-020: As a finance manager, I need tax configuration so that 
        income tax is calculated correctly for Tunisian law.
  
  Tasks:
    - Implement TaxConfigurationService (CRUD)
    - Create tax bracket configuration table
    - Configure IRPP brackets (0%, 10%, 20%, 30%)
    - Configure CNSS rate (5.95%)
    - Configure family status credits
    - Create validation for brackets
  
  Acceptance Criteria:
    - Can add tax brackets
    - Can specify min/max income for each bracket
    - Can specify tax rate and credits
    - Brackets don't overlap
    - Can have multiple years
    - Can disable old brackets

US-021: As a payroll admin, I need to configure fixed allowances so 
        that they're correctly applied to all employees.
  
  Tasks:
    - Implement AllowanceService (CRUD)
    - Create allowance configuration table
    - Configure Presence allowance (9.036 TND)
    - Configure Transport allowance (87.165 TND)
    - Configure Diligence allowance (16.011 TND)
    - Implement effectiveness dates
  
  Acceptance Criteria:
    - Can define allowances globally
    - Can set per-employee overrides
    - Can set effective dates
    - Can have multiple versions
    - Allowances versioned with audit trail

US-022: As an admin, I need to create/manage payroll periods so that 
        I can organize payroll by month.
  
  Tasks:
    - Implement PayrollPeriodService
    - Create PayrollPeriod form (month, year, dates)
    - Implement status workflow (DRAFT → LOCKED → PROCESSING → FINALIZED → PAID)
    - Validate only one period per month-year
    - Create period management UI
    - Lock periods to prevent changes
  
  Acceptance Criteria:
    - Can create payroll period for any month
    - Cannot create duplicate period
    - Can lock period to prevent changes
    - Status transitions validated
    - Only admin can create periods
    - Cannot close period while payroll is draft

**Deliverables:**
- Salary scale configuration API & UI
- Tax configuration API & UI
- Allowance configuration API & UI
- Payroll period management API & UI
- Configuration validation tests

**Risks:**
- Tax calculation complexity → test extensively against Excel
- Salary scale version management → document carefully
- Configuration changes impact on past payrolls → immutable for past periods

---

### Sprint 6: Payroll Calculation Engine (Week 12-15) ⚠️ MOST COMPLEX

**Sprint Goal:** Implement complete payroll calculation matching Excel exactly.

**User Stories:**

```
US-023: As a developer, I need base salary calculation so that monthly 
        salaries are computed correctly.
  
  Tasks:
    - Implement SalaryCalculator.calculateBaseSalary()
    - Fetch employee category & échelon
    - Lookup salary scale multiplier
    - Apply formula: Base × Multiplier
    - Implement attendance adjustment
    - Create calculation tests
  
  Test Cases:
    - Employee with échelon 1 in 2026 = base × 4.013
    - Employee with échelon 3 in 2026 = base × 4.042
    - Attendance adjustment: base × (22.5 / 26) = adjusted
    - Results match Excel to 2 decimal places

US-024: As a developer, I need allowance calculations so that all 
        compensation components are computed.
  
  Tasks:
    - Implement PresenceAllowanceCalculator (fixed × attendance ratio)
    - Implement TransportAllowanceCalculator (fixed amount)
    - Implement DiligenceAllowanceCalculator (fixed amount)
    - Implement MealAllowanceCalculator (fixed × attendance ratio)
    - Implement ChildAllowanceCalculator (by family status)
    - Create unit tests for each
  
  Test Cases:
    - Presence: 9.036 × (22.5 / 26) = 7.8196 TND
    - Transport: 87.165 TND (always)
    - Child: Depends on M1/M2/M3/C status
    - Total allowances sum correctly

US-025: As a developer, I need performance bonus calculation so that 
        ratings are converted to bonus amounts.
  
  Tasks:
    - Implement PerformanceBonusCalculator
    - Fetch monthly performance rating (7, 8, or A)
    - Apply rules:
      - 8 (Excellent) = 5%
      - 7 (Good) = 2.5%
      - A (Absent) = 0%
    - Calculate: base_salary × percentage
    - Create tests
  
  Test Cases:
    - Rating 8: base × 0.05 = bonus
    - Rating 7: base × 0.025 = bonus
    - Rating A: 0 TND
    - Missing rating: 0 TND

US-026: As a developer, I need tax calculation so that Tunisian IRPP 
        is applied correctly.
  
  Tasks:
    - Implement IRPPTaxCalculator with progressive brackets
    - Implement Tunisian 2026 tax brackets:
      - 0-2000: 0%
      - 2001-5000: 10%
      - 5001-10000: 20%
      - 10001+: 30%
    - Implement tax credits (family status + children)
    - Create detailed tests
  
  Test Cases:
    - Income 1000: tax = 0
    - Income 3000: tax = (3000-2000) × 0.10 = 100
    - Income 6000: tax = 300 + (6000-5000) × 0.20 = 500
    - Family status M3 + 3 children = credit applied

US-027: As a developer, I need CNSS calculation so that social 
        security contributions are withheld correctly.
  
  Tasks:
    - Implement CNSSCalculator (5.95% of gross)
    - Apply: gross × 0.0595
    - Create tests
  
  Test Cases:
    - Gross 1000: CNSS = 59.50
    - Gross 560.05: CNSS = 33.33
    - Results rounded to 2 decimals

US-028: As a developer, I need absence penalty calculation so that 
        missing days reduce salary.
  
  Tasks:
    - Implement AbsencePenaltyCalculator
    - Formula: Days Absent × (Base Salary / 26)
    - Create tests
  
  Test Cases:
    - 1 day absent: (560 / 26) = 21.54 TND penalty
    - 3.5 days absent: (560 / 26) × 3.5 = 75.39 TND

US-029: As a payroll admin, I need complete payroll calculation so 
        that monthly payroll is calculated for all employees.
  
  Tasks:
    - Implement PayrollCalculationService.calculatePayrollForPeriod()
    - Orchestrate all calculators (salary, allowances, deductions)
    - Handle edge cases (missing data, new hires, terminated employees)
    - Create comprehensive logging
    - Implement rollback on errors
    - Create integration tests
  
  Test Cases:
    - Calculate payroll for all 91 employees
    - Results match Excel PAIE GLOBAL sheet exactly
    - Net salary = Gross + Allowances - Deductions
    - All calculations verified against source

US-030: As a QA engineer, I need payroll calculation verification so 
        that I can confirm Excel calculations are matched exactly.
  
  Tasks:
    - Create automated reconciliation test
    - Compare calculated payroll vs Excel PAIE GLOBAL
    - Line-by-line verification
    - Generate reconciliation report
    - Create regression test suite
  
  Test Cases:
    - 100% match on net salary (to 2 decimal places)
    - All components (gross, allowances, deductions) match
    - Summary totals match
    - No discrepancies > 1 cent

**Deliverables:**
- Complete payroll calculation engine
- All calculator services (salary, allowances, deductions, tax)
- Comprehensive unit tests (200+ test cases)
- Integration test for full payroll cycle
- Reconciliation report (Excel vs System)
- Calculation documentation

**Risks:**
- Exact Excel matching difficult → test extensively
- Edge cases (new hires, terminations, probation) → handle explicitly
- Tax law complexity → get verification from finance
- Performance (91 employees calculation time) → optimize queries

---

### Sprint 7: Reports & Payslip Generation (Week 16-17)

**Sprint Goal:** Implement report generation and payslip distribution.

**User Stories:**

```
US-031: As a finance manager, I need payslip generation so that 
        employees receive detailed salary statements.
  
  Tasks:
    - Implement PayslipGenerationService
    - Create payslip PDF template (using iText or similar)
    - Include company logo, header, footer
    - Display salary components breakdown
    - Generate PDF for each employee
    - Upload PDFs to MinIO
    - Create email distribution

US-032: As a finance manager, I need to export payroll so that I can 
        analyze data in Excel or send to bank.
  
  Tasks:
    - Implement ExcelExporter
    - Create payroll summary sheet
    - Create detailed payroll sheet
    - Add charts/pivot tables
    - Format cells properly
    - Download as .xlsx

US-033: As an HR manager, I need attendance reports so that I can 
        analyze attendance patterns.
  
  Tasks:
    - Implement AttendanceReportService
    - Create summary by employee
    - Create summary by department
    - Add trends and analytics
    - Export to Excel/PDF

US-034: As a finance manager, I need tax declarations so that I can 
        file with government.
  
  Tasks:
    - Implement TaxDeclarationService
    - Generate IRPP declaration
    - Generate CNSS declaration
    - Format according to Tunisian requirements
    - Export to CSV format for government filing
    - Create validation checks

**Deliverables:**
- Payslip generation service & PDF template
- Excel export service
- Tax declaration generation
- All report exporters (Excel, PDF, CSV)
- Report tests

---

### Sprint 8: Dashboard & Analytics (Week 18)

**Sprint Goal:** Implement dashboards for different user roles.

**User Stories:**

```
US-035: As an HR manager, I need dashboard with KPIs so that I can 
        see at-a-glance metrics.
  
  Tasks:
    - Create Dashboard component
    - Display total headcount
    - Display active vs inactive
    - Display by department (pie chart)
    - Display attendance this month (%)
    - Display recent hires
    - Display org chart widget

US-036: As a finance manager, I need payroll dashboard so that I can 
        monitor payroll status.
  
  Tasks:
    - Display current payroll period
    - Display % complete
    - Display total payroll amount
    - Display approvals pending
    - Display upcoming deadlines

US-037: As an employee, I need my own dashboard so that I can see 
        my data.
  
  Tasks:
    - Display recent payslips
    - Display attendance this month
    - Display performance ratings
    - Display annual prime
    - Download payslip button

**Deliverables:**
- Multi-role dashboards
- KPI cards and charts
- Dashboard tests

---

### Sprint 9: Testing & Security (Week 19-20)

**Sprint Goal:** Comprehensive testing and security hardening.

**Technical Tasks:**

```
- Unit tests for all calculators (target: >90% coverage)
- Integration tests for payroll workflow
- API endpoint tests (all CRUD operations)
- Security tests (SQL injection, XSS, CSRF)
- Performance tests (calculate payroll for 1000 employees)
- Load tests (concurrent API requests)
- Accessibility tests (WCAG 2.1 AA compliance)
- Frontend component tests (>80% coverage)
- E2E tests (payroll cycle from start to finish)
- Security audit (OWASP Top 10)
```

**Deliverables:**
- Test coverage report (>85%)
- Security audit findings & fixes
- Performance benchmarks
- Load test results

---

### Sprint 10: Production Readiness & Launch (Week 21-22)

**Sprint Goal:** Prepare for production deployment and go-live.

**Technical Tasks:**

```
- Production database setup
- MinIO bucket setup
- SSL certificate configuration
- Environment variables documentation
- Deployment runbook creation
- Backup & recovery procedures
- Monitoring setup (logs, metrics)
- Alerting configuration
- User training materials
- Documentation finalization
- Parallel run with Excel (1 month)
- Data validation reports
- Production deployment
- Post-launch support plan
```

**Deliverables:**
- Production environment
- Deployment documentation
- Operations runbook
- Support procedures
- User training materials
- Go-live checklist

---

## 11. Testing Strategy

### 11.1 Unit Testing (Payroll Calculation Focus)

```java
@SpringBootTest
public class PayrollCalculationTest {
    
    @Test
    public void testBaseSalaryCalculation_WithEchelon3_2026() {
        // Given
        Employee employee = createEmployee(echelon=3, category="CAT3");
        PayrollPeriod period = createPeriod(year=2026, month=3);
        
        // When
        BigDecimal baseSalary = salaryCalculator.calculateBaseSalary(employee, period);
        
        // Then
        assertEquals(BigDecimal.valueOf(4042), baseSalary);
    }
    
    @Test
    public void testPresenceAllowanceCalculation_With22Point5DaysWorked() {
        // Given: 22.5 days worked out of 26
        AttendanceSummary attendance = new AttendanceSummary(daysWorked=22.5);
        Employee employee = createEmployee(presenceAllowance=9.036);
        
        // When
        BigDecimal bonus = presenceCalculator.calculate(employee, attendance);
        
        // Then
        assertEquals(BigDecimal.valueOf(7.8196), bonus);
    }
    
    @Test
    public void testPerformanceBonusCalculation_Rating8() {
        // Given
        Employee employee = createEmployee(baseSalary=560.05);
        PerformanceRating rating = new PerformanceRating(score=8);
        
        // When
        BigDecimal bonus = performanceCalculator.calculate(employee, rating);
        
        // Then
        assertEquals(BigDecimal.valueOf(28.00), bonus);
    }
    
    @Test
    public void testIRPPTaxCalculation_Income6000() {
        // Given: Income 6000 TND falls in 5001-10000 bracket
        BigDecimal income = BigDecimal.valueOf(6000);
        
        // When
        BigDecimal tax = irppCalculator.calculate(income, employee, 2026);
        
        // Then
        // 300 (from 0-5000 bracket) + (6000-5000)*0.20 = 500
        assertEquals(BigDecimal.valueOf(500), tax);
    }
    
    @Test
    public void testCNSSContribution_Gross560() {
        // Given
        BigDecimal gross = BigDecimal.valueOf(560.05);
        
        // When
        BigDecimal cnss = cnssCalculator.calculate(gross);
        
        // Then
        assertEquals(BigDecimal.valueOf(33.33), cnss);
    }
}
```

### 11.2 Integration Testing

```java
@SpringBootTest
@Transactional
public class PayrollCalculationE2ETest {
    
    @Test
    public void testCompletePayrollCalculation_ForAllEmployees() throws Exception {
        // Given: Payroll period March 2026
        PayrollPeriod period = createPayrollPeriod(2026, 3);
        List<Employee> employees = createTestEmployees(91);
        createAttendanceRecords(employees, period);
        createPerformanceRatings(employees, period);
        
        // When: Calculate payroll
        payrollCalculationService.calculatePayrollForPeriod(period.getId());
        
        // Then: Verify results
        List<Payroll> payrolls = payrollRepository.findByPeriod(period.getId());
        
        assertEquals(91, payrolls.size());
        assertAllPayrollsValid(payrolls);
        assertTotalsMatch(payrolls, expectedTotals);
        assertNoCalculationErrors(payrolls);
    }
    
    @Test
    public void testPayrollCalculationReconciliation_WithExcelData() {
        // Compare calculated payroll with Excel PAIE GLOBAL
        Payroll calculated = calculatePayrollForEmployee(...);
        ExcelPayrollRecord expected = readFromExcelFile(...);
        
        assertNetSalaryMatches(calculated, expected, delta=0.01);
        assertAllComponentsMatch(calculated, expected);
    }
}
```

### 11.3 Security Testing

```
- SQL Injection: Test that user input is properly parameterized
- XSS: Ensure all HTML content is escaped
- CSRF: Verify CSRF tokens on POST/PUT/DELETE
- Authentication: Unauthorized requests return 401
- Authorization: Insufficient permissions return 403
- Sensitive data: PII not logged or exposed
- Password security: Passwords hashed with bcrypt
```

### 11.4 Performance Testing

```
- Payroll calculation for 1000 employees: < 10 seconds
- API response time: < 500ms for 95th percentile
- Frontend page load: < 2 seconds
- Database query optimization: All queries < 100ms
- Concurrent users: Support 50 simultaneous connections
```

---

## 12. Migration Strategy

### 12.1 Historical Data Migration

**Phase 1: Data Extraction**
```sql
-- Extract from Excel sheets
SELECT * FROM Base -- Employee master
SELECT * FROM Feuil9 -- Salary scales
SELECT * FROM pointage* -- Attendance (multiple sheets)
SELECT * FROM PAIE_GLOBAL* -- Payroll (multiple sheets)
SELECT * FROM STAT_ANNUEL -- Annual statistics
```

**Phase 2: Data Validation**
- Validate CNSS numbers format
- Check for duplicate employees
- Verify date formats
- Validate salary amounts > 0
- Check attendance dates are in month range

**Phase 3: Mapping & Import**
```
Base.MLES → employees.employee_id
Base."NOM ET PRENOM" → employees.full_name
Base.DATE_NAISSANCE → employees.date_of_birth
Base.MLS_CNSS → employees.cnss_number
...
Feuil9 data → salary_scales table
pointage sheets → attendance table (by month)
PAIE GLOBAL sheets → payroll table (by month)
```

**Phase 4: Reconciliation**
- Compare total employees imported vs original
- Verify salary totals match
- Check all attendancerecords present
- Validate no data loss

### 12.2 Parallel Running (1 Month)

**Week 1-4:**
- Run both systems simultaneously
- Calculate payroll in both Excel and web platform
- Compare results line-by-line
- Fix any discrepancies

**Exit Criteria:**
- 100% match between systems (or documented exceptions)
- All users comfortable with web platform
- No critical issues found

### 12.3 Cutover Strategy

**Day 0 (Friday):**
- Final data validation
- Database backup of Excel data
- Deactivate Excel system

**Day 1 (Monday):**
- Deploy production system
- Enable users in production
- Monitor for issues

**Day 1-7:**
- Daily reconciliation with Excel
- Support team on standby
- Monitor system performance

### 12.4 Rollback Plan

**If Critical Issues Occur:**
1. Restore database from backup
2. Revert to Excel system
3. Document issues
4. Fix and re-test
5. Schedule new deployment

---

## 13. Definition of Done

### For Every Backend Service

- [ ] All methods have unit tests (>90% coverage)
- [ ] All public methods documented (Javadoc)
- [ ] All validations in place
- [ ] All errors handled with appropriate exceptions
- [ ] Audit logging implemented
- [ ] Authorization checks in place
- [ ] Database migrations created
- [ ] Integration tests pass
- [ ] API documentation (Swagger) complete
- [ ] Performance tests show acceptable times
- [ ] No code smells (SonarQube clean)

### For Every API Endpoint

- [ ] Request DTO with validation
- [ ] Response DTO created
- [ ] Error response documented
- [ ] Authorization checks enforced
- [ ] Input validation implemented
- [ ] Business logic tests written
- [ ] Integration tests written
- [ ] Swagger/OpenAPI documented
- [ ] Example payloads provided
- [ ] Edge cases tested

### For Every UI Screen

- [ ] Responsive design (mobile-compatible)
- [ ] All form fields have validation
- [ ] Error messages user-friendly
- [ ] Loading states handled
- [ ] Empty states handled
- [ ] Component tests written (>80%)
- [ ] Accessibility (WCAG 2.1 AA)
- [ ] Permissions enforced (RBAC)
- [ ] Audit trail logging
- [ ] Documentation/help text

### For Database Changes

- [ ] Flyway migration created
- [ ] Indexes created for performance
- [ ] Foreign keys enforced
- [ ] Check constraints added
- [ ] Migration tested on clean database
- [ ] Migration rollback tested
- [ ] No N+1 queries
- [ ] Query performance verified

---

## 14. Technical Debt Prevention

### Backend Code Standards

**Java/Spring Boot:**
```
- Max method length: 30 lines
- Max class size: 300 lines
- Max cyclomatic complexity: 10
- No magic numbers (use constants)
- No checked exceptions (use unchecked)
- All public methods documented
- 80%+ unit test coverage
- Logging at appropriate levels
- No System.out.println()
- Use dependency injection
- Immutable objects where possible
```

**Exception Handling:**
```java
// Good:
@ExceptionHandler(EmployeeNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(
    EmployeeNotFoundException e) {
  return ResponseEntity.status(NOT_FOUND)
    .body(new ErrorResponse("Employee not found", e.getMessage()));
}

// Bad:
try {
  // ...
} catch (Exception e) {
  e.printStackTrace();
  return null;
}
```

**Logging:**
```java
// Good:
logger.info("Payroll calculated for period {} and {} employees", 
  periodId, employeeCount);

// Bad:
logger.info("Payroll calculated");
System.out.println("Done");
```

### Frontend Code Standards

**React/TypeScript:**
```
- Functional components (no class components)
- Custom hooks for reusable logic
- Prop validation with TypeScript
- Event handlers namespaced (handleClick, onSubmit)
- No inline functions (use callbacks)
- Memoization for expensive components
- Error boundaries around feature areas
- Loading skeletons for async data
- Proper cleanup in useEffect
```

**Component Organization:**
```typescript
// Good component structure:
interface ComponentProps {
  items: Item[];
  onSelect: (id: number) => void;
}

export const ItemList: React.FC<ComponentProps> = ({ 
  items, 
  onSelect 
}) => {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  
  const handleSelect = (id: number) => {
    setSelectedId(id);
    onSelect(id);
  };
  
  return (
    <div>
      {items.map(item => (
        <ItemRow 
          key={item.id}
          item={item}
          onSelect={handleSelect}
        />
      ))}
    </div>
  );
};
```

### API Versioning

```
/api/v1/employees
/api/v1/payroll
/api/v1/attendance

No breaking changes in v1
Create v2 for major changes
Deprecate v1 gradually
```

### Database Standards

```sql
-- Column naming
employee_id (not employeeId)
first_name (not firstName)
is_active (not active)
created_at (not createdDate)

-- Always include audit columns
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
created_by VARCHAR(100)
updated_by VARCHAR(100)

-- Indexes on foreign keys and frequently queried fields
CREATE INDEX idx_employees_department 
  ON employees(department_id);

-- Check constraints for business rules
CHECK (echelon >= 1 AND echelon <= 14)
CHECK (base_salary > 0)
```

---

## Conclusion & Next Steps

This implementation roadmap provides a complete technical blueprint for building the MARAM Confection Payroll Platform. The 10-sprint structure spans approximately 22 weeks and can be executed by a team of 3-4 developers with 1 QA engineer.

**Key Success Factors:**

1. **Exact Excel Matching** - Payroll calculation MUST match Excel to 2 decimal places
2. **Comprehensive Testing** - Every calculation has automated tests
3. **Data Integrity** - ACID compliance and audit trails on all changes
4. **User Training** - HR and Finance teams fully trained before go-live
5. **Parallel Running** - 4-week verification period running both systems

**Success Criteria:**
- ✅ 100% calculation accuracy
- ✅ 100% data integrity
- ✅ > 90% user adoption within 30 days
- ✅ Zero critical production issues
- ✅ 99.9% uptime
- ✅ < 2 hour payroll processing time

This roadmap is ready for immediate development start.

---

**Document Version:** 1.0  
**Status:** Ready for Development  
**Last Updated:** June 2026
