-- ============================================================
-- V2 — Salary configuration, allowances & payroll periods
-- ============================================================

-- Salary scales: multiplier by category × echelon × year
CREATE TABLE salary_scales (
    id                BIGSERIAL PRIMARY KEY,
    category_id       BIGINT NOT NULL REFERENCES salary_categories (id),
    echelon           INT NOT NULL CHECK (echelon BETWEEN 1 AND 14),
    year              INT NOT NULL,
    salary_multiplier DECIMAL(5, 3) NOT NULL,
    annual_salary     DECIMAL(10, 2),
    increase_amount   DECIMAL(10, 2),
    increase_percent  DECIMAL(5, 2),
    valid_from        DATE NOT NULL,
    valid_to          DATE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (category_id, echelon, year)
);

CREATE INDEX idx_salary_scales_year ON salary_scales (year);
CREATE INDEX idx_salary_scales_valid_dates ON salary_scales (valid_from, valid_to);

-- Per-employee allowances
CREATE TABLE allowances (
    id             BIGSERIAL PRIMARY KEY,
    employee_id    BIGINT NOT NULL REFERENCES employees (id),
    allowance_type VARCHAR(50) NOT NULL,  -- PRESENCE, TRANSPORT, DILIGENCE, MEAL, CHILD, OTHER
    amount         DECIMAL(10, 2) NOT NULL,
    frequency      VARCHAR(20) DEFAULT 'MONTHLY',
    is_fixed       BOOLEAN DEFAULT TRUE,
    effective_date DATE NOT NULL,
    end_date       DATE,
    notes          TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_allowances_employee ON allowances (employee_id);
CREATE INDEX idx_allowances_type ON allowances (allowance_type);
CREATE INDEX idx_allowances_effective ON allowances (effective_date, end_date);

-- Monthly payroll periods
CREATE TABLE payroll_periods (
    id                 BIGSERIAL PRIMARY KEY,
    period_code        VARCHAR(20) UNIQUE NOT NULL,  -- e.g. 2026-03
    period_month       INT NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    period_year        INT NOT NULL,
    start_date         DATE NOT NULL,
    end_date           DATE NOT NULL,
    working_days       INT DEFAULT 26,
    holidays_in_period INT DEFAULT 0,
    status             VARCHAR(20) DEFAULT 'DRAFT',  -- DRAFT, LOCKED, PROCESSING, FINALIZED, PAID
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at       TIMESTAMP,
    approved_by        VARCHAR(100),
    approved_at        TIMESTAMP,
    paid_at            TIMESTAMP,
    UNIQUE (period_year, period_month)
);

CREATE INDEX idx_payroll_periods_code ON payroll_periods (period_code);
CREATE INDEX idx_payroll_periods_year_month ON payroll_periods (period_year, period_month);
CREATE INDEX idx_payroll_periods_status ON payroll_periods (status);
