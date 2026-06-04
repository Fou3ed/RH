# Prime Annuel 2025 - Excel Analysis & Web Platform Architecture

## Executive Summary

This document provides a comprehensive analysis of the **"prime annuel 2025.xlsm"** Excel workbook and proposes a modern **ERP/CRM system** to replace manual spreadsheet operations with an automated, database-driven web platform.

---

## Part 1: Current System Analysis

### 1.1 Overview
The Excel file is a **Human Resources & Payroll Management System** for **MARAM CONFECTION** (a textile/manufacturing company). It handles:
- Employee master data
- Payroll calculations
- Performance ratings
- Staff statistics and analytics
- Attendance tracking
- Bonus/prime calculations

---

## Part 2: Detailed Sheet Breakdown

### Sheet 1: **Feuil1** - Employee Master Data
**Purpose:** Central employee database with comprehensive information

**Key Columns:**
- `n°` - Employee sequential number
- `Mles` - Employee ID
- `Nom et Prénom` - Full name
- `Qualification/Cat` - Job title and category
- `Date Emb.` - Hiring date
- `Ech` - Grade/échelon level
- `S.B/T.H` - Base salary or hourly rate
- `Ancienneté` - Seniority in years
- **Performance Ratings** - Multiple columns with scores (7, 8, A ratings) representing:
  - Monthly evaluations
  - Departmental assignments
  - Performance metrics

**Current Issues:**
- Manual data entry prone to errors
- No real-time updates
- Calculations done outside the system
- Difficult to track historical data
- No audit trail

**Total Employees:** ~91 employees (with distribution across departments)

---

### Sheet 2: **Feuil9** - Salary Scale Reference Table
**Purpose:** Define automatic salary increments and brackets

**Structure:**
```
ECHELON F | 1 | 2 | 3 | 4 | 5 | ... | 14
2025 CAT 3| 3.751 | 3.761 | 3.766 | ... | 3.934
2026 CAT 3| 4.013 | 4.024 | 4.030 | ... | 4.210
Augmentation| 0.262 | 0.263 | 0.264 | ... | 0.276
%         | 6.98% | 6.99% | 7.01% | ... | 7.01%
```

**Key Insights:**
- Multiple employee categories/grades
- Annual salary progression defined
- Percentage increases (6.98% - 7.01% for 2025-2026)
- 14 échelon levels per category
- Should be stored in database for automatic calculation

---

### Sheet 3: **PAIE GLOBAL JANV 26** - Global Payroll (January 2026)
**Purpose:** Monthly payroll register for the entire organization

**Structure:**
- Individual employee payroll records
- Multiple compensation columns
- Deductions (likely)
- Net pay calculations
- Tax withholdings
- Benefits

**Key Challenge:** Currently manual creation each month - perfect candidate for automation

---

### Sheet 4: **stat sur ETAT PERSONNEL (2)** - Staff Statistics & Demographics
**Purpose:** Comprehensive workforce analysis and reporting

**Analysis Sections:**

#### A. Gender Distribution
- **Female:** 83.9% (76+ employees)
- **Male:** 16.1% (~15 employees)
- **Insight:** Highly feminized workforce (typical for textile manufacturing)

#### B. Position Breakdown by Department
| Position | Count | % |
|----------|-------|-----|
| OUVRIERE/MACHINE | 34 | 37.4% |
| PLIEUSE ENSACHEUSE | 10 | 11.0% |
| REPASSAGE | 7 | 7.7% |
| COUPE FIL | 5 | 5.5% |
| CONTRÔLE QUALITÉ | 5 | 5.5% |
| MONITRICE CLASSE 1 | 3 | 3.3% |
| LANCEMENT | 2 | 2.2% |
| CHEF DE COUPE | 2 | 2.2% |
| And others... | | |

#### C. Age Group Analysis
- Age brackets defined (e.g., "Moins que 1 AN", "Plus que 1 A", "20-30", "30-40", "40-50", "50-60")
- Workforce composition by age
- Key for succession planning & HR strategy

#### D. Management Analysis
- Attachment relationships between supervisors and staff
- Team structure visualization
- Reporting hierarchies

**Current State Issues:**
- Manual data compilation
- Outdated as soon as created
- Cannot slice/dice data dynamically

---

### Sheet 5: **Feuil8** - Attendance & Hours Tracking
**Purpose:** Time tracking and attendance management

**Key Columns:**
- Employee ID & Name
- Hours worked (PTAGE column)
- Department/Position
- Status (ABS = Absent, PARTANT = Departing/Leaving)
- Hourly rate or performance multiplier
- Notes/Flags (e.g., "D OU ND ??")

**Current Issues:**
- Manual entry
- No validation
- Difficult to calculate overtime or deductions
- Cannot correlate with performance ratings

---

## Part 3: Current Business Logic & Calculations

### 3.1 Payroll Calculations
**What's Currently Done in Excel:**
1. Load employee base salary from master data
2. Apply échelon multiplier based on seniority
3. Calculate monthly salary = Base × Échelon Factor
4. Apply attendance/hours deductions
5. Calculate performance-based bonuses
6. Apply statutory deductions (taxes, social security)
7. Generate payslips

### 3.2 Annual Prime (Bonus)
- Based on seniority, performance ratings, and category
- Currently calculated manually
- Should be automated based on defined rules

### 3.3 Performance Management
- Rating system: 7, 8, or A (absent/not rated)
- Monthly tracking across different competencies
- Used for bonus calculations

### 3.4 HR Analytics
- Workforce demographics
- Turnover tracking
- Department statistics
- Compliance reporting

---

## Part 4: Proposed Web Platform Architecture

### 4.1 System Overview: ERP/CRM for HR & Payroll

```
┌─────────────────────────────────────────────────────────────┐
│                    WEB PLATFORM                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐  ┌─────────────────┐  ┌────────────┐ │
│  │ Dashboard/       │  │ Employee        │  │ Payroll    │ │
│  │ Analytics        │  │ Management      │  │ Module     │ │
│  └──────────────────┘  └─────────────────┘  └────────────┘ │
│                                                              │
│  ┌──────────────────┐  ┌─────────────────┐  ┌────────────┐ │
│  │ Attendance       │  │ Performance     │  │ Reporting  │ │
│  │ Tracking         │  │ Management      │  │ & Export   │ │
│  └──────────────────┘  └─────────────────┘  └────────────┘ │
│                                                              │
│  ┌──────────────────┐  ┌─────────────────┐  ┌────────────┐ │
│  │ Department       │  │ User/Auth       │  │ System     │ │
│  │ Management       │  │ Management      │  │ Settings   │ │
│  └──────────────────┘  └─────────────────┘  └────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────┐
│               BUSINESS LOGIC LAYER                           │
├─────────────────────────────────────────────────────────────┤
│  • Payroll Calculation Engine                               │
│  • Performance Evaluation Logic                             │
│  • Bonus/Prime Calculation                                  │
│  • Compliance & Validation                                  │
│  • Report Generation                                        │
└─────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────┐
│                  DATABASE LAYER                             │
├─────────────────────────────────────────────────────────────┤
│  • PostgreSQL / MySQL                                       │
│  • Normalized schema with audit trails                      │
│  • Historical data retention                                │
│  • Backup & recovery capabilities                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Part 5: Proposed Database Schema

### 5.1 Core Tables

#### **employees**
```sql
CREATE TABLE employees (
  id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id VARCHAR(10) UNIQUE,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  gender ENUM('M', 'H') NOT NULL,
  date_of_birth DATE,
  hire_date DATE NOT NULL,
  department_id INT NOT NULL,
  position_id INT NOT NULL,
  category_id INT NOT NULL,
  echelon INT DEFAULT 1,
  base_salary DECIMAL(10, 2) NOT NULL,
  hourly_rate DECIMAL(8, 2),
  seniority_years INT,
  status ENUM('active', 'inactive', 'leave', 'terminated'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (department_id) REFERENCES departments(id),
  FOREIGN KEY (position_id) REFERENCES positions(id),
  FOREIGN KEY (category_id) REFERENCES salary_categories(id)
);
```

#### **salary_categories**
```sql
CREATE TABLE salary_categories (
  id INT PRIMARY KEY AUTO_INCREMENT,
  category_code VARCHAR(20) UNIQUE,
  description VARCHAR(255),
  base_multiplier DECIMAL(5, 3),
  created_at TIMESTAMP
);
```

#### **salary_scales**
```sql
CREATE TABLE salary_scales (
  id INT PRIMARY KEY AUTO_INCREMENT,
  category_id INT NOT NULL,
  echelon INT NOT NULL,
  year INT NOT NULL,
  salary_amount DECIMAL(10, 2) NOT NULL,
  increase_percent DECIMAL(5, 2),
  FOREIGN KEY (category_id) REFERENCES salary_categories(id),
  UNIQUE(category_id, echelon, year)
);
```

#### **departments**
```sql
CREATE TABLE departments (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  manager_id INT,
  created_at TIMESTAMP
);
```

#### **positions**
```sql
CREATE TABLE positions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  department_id INT,
  salary_level INT,
  FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

#### **attendance**
```sql
CREATE TABLE attendance (
  id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  date DATE NOT NULL,
  hours_worked DECIMAL(5, 2),
  status ENUM('present', 'absent', 'leave', 'partant') DEFAULT 'present',
  notes TEXT,
  created_at TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id),
  UNIQUE(employee_id, date)
);
```

#### **performance_ratings**
```sql
CREATE TABLE performance_ratings (
  id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  period_month INT,
  period_year INT,
  rating INT DEFAULT 7,  -- 7, 8, or A
  notes TEXT,
  rater_id INT,
  created_at TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id),
  FOREIGN KEY (rater_id) REFERENCES employees(id)
);
```

#### **payroll**
```sql
CREATE TABLE payroll (
  id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  period_month INT,
  period_year INT,
  base_salary DECIMAL(10, 2),
  attendance_deduction DECIMAL(10, 2),
  performance_bonus DECIMAL(10, 2),
  other_allowances DECIMAL(10, 2),
  tax_deduction DECIMAL(10, 2),
  social_security DECIMAL(10, 2),
  net_salary DECIMAL(10, 2),
  status ENUM('draft', 'approved', 'paid', 'cancelled'),
  created_at TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id),
  UNIQUE(employee_id, period_month, period_year)
);
```

#### **annual_prime**
```sql
CREATE TABLE annual_prime (
  id INT PRIMARY KEY AUTO_INCREMENT,
  employee_id INT NOT NULL,
  year INT NOT NULL,
  base_amount DECIMAL(10, 2),
  seniority_bonus DECIMAL(10, 2),
  performance_bonus DECIMAL(10, 2),
  total_prime DECIMAL(10, 2),
  payment_date DATE,
  status ENUM('calculated', 'approved', 'paid'),
  created_at TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id),
  UNIQUE(employee_id, year)
);
```

---

## Part 6: Platform Features & Modules

### 6.1 Dashboard
- **Real-time KPIs:**
  - Total headcount
  - Active vs. inactive employees
  - Department breakdown
  - Payroll status
  - Pending approvals

- **Visual Analytics:**
  - Gender distribution charts
  - Age group distribution
  - Department headcount
  - Turnover rate
  - Salary ranges by position

### 6.2 Employee Management Module
**Features:**
- Add/Edit/Delete employee records
- Employee profile with full history
- Document attachment (contracts, certifications)
- Department & position assignment
- Category and échelon management
- Bulk import from CSV/Excel
- Export employee lists

**Workflows:**
- New hire onboarding
- Promotion tracking
- Termination process
- Leave management

### 6.3 Payroll Module
**Features:**
- **Automated Payroll Generation:**
  - Monthly payroll run
  - Automatic salary calculation based on échelon
  - Deduction calculations (taxes, social security)
  - Attendance-based adjustments
  - Performance bonus application

- **Salary Configuration:**
  - Define salary scales by category & year
  - Set échelon progression rules
  - Configure tax brackets & deductions
  - Define bonus formulas

- **Payslip Generation:**
  - Individual payslips (PDF)
  - Batch payslip generation
  - Email distribution to employees
  - Archive for compliance

- **Approval Workflow:**
  - Draft → Review → Approve → Payment
  - Multi-level approval (Manager → HR → Finance)
  - Audit trail of all changes

### 6.4 Attendance Tracking Module
**Features:**
- **Daily Clock-in/Clock-out:**
  - Mobile/web interface
  - Geolocation (optional)
  - Biometric integration (future)

- **Attendance Reports:**
  - Daily presence sheets
  - Monthly attendance summary
  - Absence tracking
  - Leave requests

- **Integration with Payroll:**
  - Auto-deduct for absences
  - Adjust salary based on hours

### 6.5 Performance Management Module
**Features:**
- **Monthly Ratings:**
  - Rate employees on defined criteria
  - Scale: 7, 8, A
  - Comments & feedback

- **Performance History:**
  - Track ratings over time
  - Identify trends
  - Performance vs. compensation analysis

- **Bonus Calculation:**
  - Automatic bonus based on performance
  - Override capability with justification

### 6.6 Annual Prime (Bonus) Module
**Features:**
- **Prime Calculation Engine:**
  - Input: Base salary, seniority, performance ratings, year
  - Output: Annual prime amount
  - Configurable formula

- **Approval & Payment:**
  - Calculate for all employees
  - Approval workflow
  - Payment scheduling
  - Tax reporting

- **Historical Comparison:**
  - Year-over-year comparison
  - Fairness analysis
  - Budget tracking

### 6.7 Reporting & Analytics Module
**Features:**
- **Pre-built Reports:**
  - Payroll summary (monthly/quarterly/annual)
  - Headcount report
  - Departmental breakdown
  - Salary statistics
  - Turnover analysis
  - Gender/age distribution
  - Compliance reports

- **Custom Report Builder:**
  - Drag-and-drop report creation
  - Filter & group by any field
  - Export to PDF, Excel, CSV

- **Data Visualization:**
  - Charts & graphs
  - Trends over time
  - Comparative analysis
  - KPI dashboards

### 6.8 Department Management
**Features:**
- Define departments & sub-departments
- Assign managers
- Track headcount per department
- Department budgets (future)
- Organizational hierarchy view

### 6.9 User & Security Management
**Features:**
- Role-based access control (RBAC)
- Roles: Admin, HR Manager, Finance, Manager, Employee
- Permissions matrix
- Audit logging
- Login history
- Change audit trail

**Security:**
- Encryption for sensitive data
- Password policies
- Two-factor authentication (optional)
- Data backup & recovery
- Compliance with local labor laws

---

## Part 7: Technology Stack Recommendation

### Frontend
- **Framework:** React.js or Vue.js
- **UI Library:** Material-UI, Tailwind CSS, or Bootstrap
- **State Management:** Redux or Vuex
- **Charts:** Chart.js, D3.js, or Recharts
- **Reports:** jsPDF, html2pdf for PDF generation

### Backend
- **Language:** Node.js (Express.js) or Python (Django/Flask)
- **Database:** PostgreSQL (recommended for relational data integrity)
- **Authentication:** JWT, OAuth 2.0
- **API:** RESTful API or GraphQL
- **Background Jobs:** Bull, Celery (for payroll automation)
- **File Storage:** AWS S3 or local storage

### DevOps & Infrastructure
- **Version Control:** Git (GitHub/GitLab)
- **Containerization:** Docker
- **Orchestration:** Docker Compose or Kubernetes
- **CI/CD:** GitHub Actions, GitLab CI, or Jenkins
- **Hosting:** AWS, Digital Ocean, or Linode
- **Monitoring:** Prometheus, Grafana, ELK Stack

---

## Part 8: Implementation Roadmap

### Phase 1: MVP (2-3 months)
- [ ] Database design & setup
- [ ] Employee management module
- [ ] Attendance tracking (manual entry)
- [ ] Basic payroll calculation
- [ ] Dashboard with basic KPIs
- [ ] User authentication & roles

### Phase 2: Enhancement (1-2 months)
- [ ] Performance management module
- [ ] Annual prime calculation
- [ ] Payslip generation & distribution
- [ ] Reporting module
- [ ] Advanced analytics

### Phase 3: Automation & Integration (1-2 months)
- [ ] Automated payroll run (scheduled)
- [ ] API for third-party integrations
- [ ] Bank integration (payment processing)
- [ ] Email automation
- [ ] Mobile app (basic)

### Phase 4: Advanced Features (ongoing)
- [ ] Leave management
- [ ] Recruitment module
- [ ] Training & development tracking
- [ ] Performance appraisals
- [ ] Compliance reporting (government requirements)
- [ ] Biometric integration
- [ ] Multi-language support

---

## Part 9: Migration Plan from Excel

### Step 1: Data Validation
- Clean existing Excel data
- Identify inconsistencies
- Map to new database schema

### Step 2: Data Import
- Create import scripts
- Validate imported data
- Handle historical data

### Step 3: Parallel Running
- Run both systems simultaneously for 1-2 months
- Compare results
- Build confidence

### Step 4: Full Migration
- Cutover to new system
- Archive Excel files
- Archive as backups only

### Step 5: Optimization
- Monitor system performance
- Get user feedback
- Iterate and improve

---

## Part 10: Key Business Rules & Formulas

### Payroll Calculation
```
Monthly Salary = Base Salary × Salary Scale Factor (by Échelon & Category & Year)

Net Salary = Monthly Salary 
            - Attendance Deductions 
            + Performance Bonus 
            + Other Allowances 
            - Tax Withholding 
            - Social Security Contributions
```

### Attendance Deduction
```
If Hours Worked < Standard Hours:
  Hourly Rate = Monthly Salary / 30 / 8  (assuming 8-hour days)
  Deduction = (Standard Hours - Hours Worked) × Hourly Rate
```

### Performance Bonus
```
If Rating = 8:
  Performance Bonus = Monthly Salary × 5%
Else If Rating = 7:
  Performance Bonus = Monthly Salary × 2.5%
Else If Rating = A (Absent):
  Performance Bonus = 0
```

### Annual Prime
```
Annual Prime = Base Salary 
             + (Seniority Bonus = Base Salary × Years Service × %)
             + (Performance Bonus = Average Monthly Performance × 12)
```

---

## Part 11: Benefits of the Web Platform

| Aspect | Excel | Web Platform |
|--------|-------|--------------|
| **Data Entry Speed** | Manual, slow | Automated forms, bulk import |
| **Error Reduction** | High error rate | Validation rules, error checking |
| **Real-time Data** | End-of-month snapshots | Live data always available |
| **Calculations** | Manual, prone to errors | Automated, 100% accurate |
| **Reporting** | Manual, time-consuming | Instant, customizable reports |
| **Data Security** | Limited control | Encryption, access control, audit logs |
| **Scalability** | Breaks at ~1000 rows | Handles millions of records |
| **Collaboration** | Version conflicts | Real-time collaboration |
| **Compliance** | Difficult to audit | Complete audit trail |
| **Cost** | Licenses, manual labor | One-time investment |
| **Employee Self-Service** | Not possible | Payslip download, data viewing |
| **Forecasting** | Manual, inaccurate | Built-in analytics, predictions |

---

## Part 12: Cost-Benefit Analysis

### Costs
- **Development:** $20,000 - $50,000 (depending on scope)
- **Infrastructure:** $500 - $2,000/month
- **Training:** $2,000 - $5,000
- **Maintenance:** 10-20% of development cost per year

### Benefits
- **Time Savings:** 40-50 hours/month (HR team)
- **Error Reduction:** 95% fewer payroll errors
- **Compliance:** Reduced penalties & legal issues
- **Efficiency:** Payroll processed in hours, not days
- **Analytics:** Better insights for strategic decisions
- **Scalability:** Ready for growth without tool changes
- **ROI:** ~6-12 months payback period

---

## Part 13: Next Steps

1. **Validate Requirements:** Confirm all business processes with stakeholders
2. **Get Approval:** Present to management for funding approval
3. **Hire/Assign Development Team:** Internal or external contractor
4. **Create Detailed Specifications:** Build detailed requirements document
5. **Database Design Review:** Finalize schema with stakeholders
6. **Start Development:** Begin MVP development
7. **Plan Testing:** Create test cases and QA process
8. **Plan Training:** Develop training materials for users
9. **Prepare Migration:** Plan data migration strategy

---

## Conclusion

The current Excel-based system serves the organization's immediate needs but is reaching its limits. A web-based ERP/CRM platform would provide:

✅ **Automation** - Reduce manual work by 80%
✅ **Accuracy** - Eliminate human error in calculations
✅ **Scalability** - Support future growth without tool changes
✅ **Visibility** - Real-time dashboards and analytics
✅ **Compliance** - Audit trails and regulatory reporting
✅ **Efficiency** - Faster payroll cycles and reporting
✅ **Security** - Protected employee and financial data

This is a strategic investment in the organization's operational efficiency and data integrity.

---

**Document Version:** 1.0
**Last Updated:** June 2026
**Status:** Recommendation Phase
