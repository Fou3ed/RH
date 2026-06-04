-- ============================================================
-- V1 — Base schema: organisational structure & employee master data
-- ============================================================

-- Departments
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    manager_id  BIGINT REFERENCES departments (id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

CREATE INDEX idx_departments_code ON departments (code);
CREATE INDEX idx_departments_name ON departments (name);

-- Positions
CREATE TABLE positions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20) UNIQUE NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    department_id BIGINT NOT NULL REFERENCES departments (id),
    salary_grade  INT CHECK (salary_grade BETWEEN 1 AND 14),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_positions_code ON positions (code);
CREATE INDEX idx_positions_department ON positions (department_id);

-- Salary categories (CAT 1, CAT 3, ...)
CREATE TABLE salary_categories (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) UNIQUE NOT NULL,
    name            VARCHAR(100),
    description     TEXT,
    base_multiplier DECIMAL(5, 3) DEFAULT 1.000,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Employees
CREATE TABLE employees (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         VARCHAR(20) UNIQUE NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    full_name           VARCHAR(200) NOT NULL,
    date_of_birth       DATE,
    gender              CHAR(1),                 -- M / H
    hire_date           DATE NOT NULL,
    department_id       BIGINT NOT NULL REFERENCES departments (id),
    position_id         BIGINT REFERENCES positions (id),
    category_id         BIGINT NOT NULL REFERENCES salary_categories (id),
    echelon             INT DEFAULT 1 CHECK (echelon BETWEEN 1 AND 14),
    base_salary         DECIMAL(10, 2),
    hourly_rate         DECIMAL(8, 2),
    family_status       VARCHAR(10),             -- C, M1, M2, M3
    number_of_children  INT DEFAULT 0,
    national_id         VARCHAR(20) UNIQUE,
    cnss_number         VARCHAR(20) UNIQUE,
    phone_number        VARCHAR(20),
    email               VARCHAR(100),
    address             VARCHAR(255),
    city                VARCHAR(100),
    zip_code            VARCHAR(10),
    payment_method      VARCHAR(20),             -- BANK_TRANSFER / CASH
    bank_account_number VARCHAR(30),
    bank_code           VARCHAR(10),
    employment_status   VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, LEAVE, TERMINATED
    termination_date    DATE,
    termination_reason  VARCHAR(255),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX idx_employees_id ON employees (employee_id);
CREATE INDEX idx_employees_department ON employees (department_id);
CREATE INDEX idx_employees_status ON employees (employment_status);
CREATE INDEX idx_employees_hire_date ON employees (hire_date);
CREATE INDEX idx_employees_email ON employees (email);
-- Full-text search over the employee name (PostgreSQL GIN index)
CREATE INDEX idx_employees_search ON employees USING gin (to_tsvector('simple', full_name));
