-- ============================================================
-- V11 — Global (company-wide) allowance configuration.
-- The existing `allowances` table holds per-employee overrides; this table holds
-- the default amounts the payroll engine applies to everyone.
-- ============================================================

CREATE TABLE allowance_config (
    id                  BIGSERIAL PRIMARY KEY,
    allowance_type      VARCHAR(50) NOT NULL,   -- PRESENCE, TRANSPORT, DILIGENCE, MEAL, CHILD
    amount              DECIMAL(10, 3) NOT NULL,
    attendance_adjusted BOOLEAN NOT NULL DEFAULT FALSE, -- scaled by days_worked/26 when true
    effective_date      DATE NOT NULL,
    end_date            DATE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_allowance_config_type ON allowance_config (allowance_type);
CREATE INDEX idx_allowance_config_effective ON allowance_config (effective_date, end_date);

-- Seed the known fixed allowances (values from the reverse-engineered Excel; confirm with finance).
INSERT INTO allowance_config (allowance_type, amount, attendance_adjusted, effective_date) VALUES
    ('PRESENCE',  9.036,  TRUE,  '2026-01-01'),
    ('TRANSPORT', 87.165, FALSE, '2026-01-01'),
    ('DILIGENCE', 16.011, FALSE, '2026-01-01'),
    ('MEAL',      22.500, TRUE,  '2026-01-01'),
    ('CHILD',     5.000,  FALSE, '2026-01-01');
