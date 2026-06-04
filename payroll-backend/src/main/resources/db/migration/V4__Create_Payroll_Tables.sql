-- ============================================================
-- V4 — Payroll transactions, tax config, performance & annual prime
-- ============================================================

-- Main monthly payroll record (one row per employee per period)
CREATE TABLE payroll (
    id                  BIGSERIAL PRIMARY KEY,
    payroll_period_id   BIGINT NOT NULL REFERENCES payroll_periods (id),
    employee_id         BIGINT NOT NULL REFERENCES employees (id),
    days_worked         DECIMAL(5, 2) NOT NULL,
    attendance_rate     DECIMAL(5, 4),
    base_salary         DECIMAL(10, 2) NOT NULL,
    adjusted_salary     DECIMAL(10, 2),

    -- Allowances
    presence_allowance  DECIMAL(10, 2) DEFAULT 0,
    transport_allowance DECIMAL(10, 2) DEFAULT 0,
    diligence_allowance DECIMAL(10, 2) DEFAULT 0,
    meal_allowance      DECIMAL(10, 2) DEFAULT 0,
    child_allowance     DECIMAL(10, 2) DEFAULT 0,
    performance_bonus   DECIMAL(10, 2) DEFAULT 0,
    other_allowances    DECIMAL(10, 2) DEFAULT 0,
    total_allowances    DECIMAL(10, 2) DEFAULT 0,

    gross_salary        DECIMAL(10, 2) NOT NULL,

    -- Deductions
    absence_penalty     DECIMAL(10, 2) DEFAULT 0,
    income_tax_irpp     DECIMAL(10, 2) DEFAULT 0,
    cnss_contribution   DECIMAL(10, 2) DEFAULT 0,
    health_insurance    DECIMAL(10, 2) DEFAULT 0,
    loan_repayment      DECIMAL(10, 2) DEFAULT 0,
    other_deductions    DECIMAL(10, 2) DEFAULT 0,
    total_deductions    DECIMAL(10, 2) DEFAULT 0,

    net_salary          DECIMAL(10, 2) NOT NULL,

    payment_status      VARCHAR(20) DEFAULT 'DRAFT',  -- DRAFT, APPROVED, PAID, CANCELLED
    payment_date        DATE,
    payment_reference   VARCHAR(100),

    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    approved_by         VARCHAR(100),
    approved_at         TIMESTAMP,
    UNIQUE (employee_id, payroll_period_id)
);

CREATE INDEX idx_payroll_period ON payroll (payroll_period_id);
CREATE INDEX idx_payroll_employee ON payroll (employee_id);
CREATE INDEX idx_payroll_status ON payroll (payment_status);

-- Tax configuration (IRPP brackets, CNSS rate, health rate)
CREATE TABLE tax_configuration (
    id                  BIGSERIAL PRIMARY KEY,
    tax_year            INT NOT NULL,
    tax_type            VARCHAR(50) NOT NULL,  -- IRPP, CNSS, HEALTH
    min_taxable_income  DECIMAL(10, 2),
    max_taxable_income  DECIMAL(10, 2),
    tax_rate            DECIMAL(5, 2),
    tax_credit_amount   DECIMAL(10, 2),
    family_status_code  VARCHAR(10),
    number_of_children  INT,
    effective_date      DATE NOT NULL,
    end_date            DATE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tax_config_year_type ON tax_configuration (tax_year, tax_type);
CREATE INDEX idx_tax_config_effective ON tax_configuration (effective_date, end_date);

-- Monthly performance ratings (basis for the performance bonus)
CREATE TABLE performance_ratings (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT NOT NULL REFERENCES employees (id),
    payroll_period_id BIGINT NOT NULL REFERENCES payroll_periods (id),
    rating_score      INT CHECK (rating_score IN (0, 7, 8)),  -- 8=excellent, 7=good, 0=absent
    rating_category   VARCHAR(50),                            -- EXCELLENT, GOOD, ABSENT
    comments          TEXT,
    rater_id          BIGINT REFERENCES employees (id),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, payroll_period_id)
);

CREATE INDEX idx_performance_employee ON performance_ratings (employee_id);
CREATE INDEX idx_performance_period ON performance_ratings (payroll_period_id);

-- End-of-year prime
CREATE TABLE annual_prime (
    id                         BIGSERIAL PRIMARY KEY,
    employee_id                BIGINT NOT NULL REFERENCES employees (id),
    prime_year                 INT NOT NULL,
    base_amount                DECIMAL(10, 2) NOT NULL,
    seniority_years            INT,
    seniority_bonus            DECIMAL(10, 2) DEFAULT 0,
    average_performance_rating DECIMAL(3, 2),
    performance_bonus          DECIMAL(10, 2) DEFAULT 0,
    total_prime                DECIMAL(10, 2),
    calculation_date           TIMESTAMP,
    approval_status            VARCHAR(20) DEFAULT 'CALCULATED', -- CALCULATED, APPROVED, REJECTED
    approved_by                VARCHAR(100),
    approved_date              TIMESTAMP,
    payment_status             VARCHAR(20) DEFAULT 'PENDING',    -- PENDING, PAID, CANCELLED
    payment_date               DATE,
    payment_reference          VARCHAR(100),
    created_at                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, prime_year)
);

CREATE INDEX idx_annual_prime_employee ON annual_prime (employee_id);
CREATE INDEX idx_annual_prime_year ON annual_prime (prime_year);
