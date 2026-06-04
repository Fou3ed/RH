-- ============================================================
-- V12 — Replace the placeholder IRPP config with the REAL Tunisian annual
-- barème reverse-engineered from the MARAM workbook (MATRICE IMPOT / ABATTEMENT).
--
-- IRPP is computed on ANNUAL net taxable income:
--   annualNetTaxable = (gross - cnss) * 12  - professional abatement - family abatement
--   annualTax        = progressive barème(annualNetTaxable)
--   monthlyIrpp      = annualTax / 12
--
--  * tax_type IRPP          → progressive brackets (annual bounds, marginal rate %)
--  * tax_type ABATEMENT     → family income abatement (annual), keyed by family_status_code
--  * tax_type ABATEMENT_PRO → professional expenses abatement: tax_rate=%, tax_credit_amount=annual cap
-- ============================================================

-- Drop the Sprint-1 placeholder IRPP brackets for 2026.
DELETE FROM tax_configuration WHERE tax_year = 2026 AND tax_type = 'IRPP';

-- Real annual IRPP barème (Tunisia).
INSERT INTO tax_configuration (tax_year, tax_type, min_taxable_income, max_taxable_income, tax_rate, effective_date) VALUES
    (2026, 'IRPP',     0.00,     1500.00,  0.00, '2026-01-01'),
    (2026, 'IRPP',  1500.00,     5000.00, 15.00, '2026-01-01'),
    (2026, 'IRPP',  5000.00,    10000.00, 20.00, '2026-01-01'),
    (2026, 'IRPP', 10000.00,    20000.00, 25.00, '2026-01-01'),
    (2026, 'IRPP', 20000.00,    50000.00, 30.00, '2026-01-01'),
    (2026, 'IRPP', 50000.00, 99999999.00, 35.00, '2026-01-01');

-- Family income abatements (annual): chef de famille 300 DT + 100 DT per child (capped at M4).
INSERT INTO tax_configuration (tax_year, tax_type, family_status_code, tax_credit_amount, effective_date) VALUES
    (2026, 'ABATEMENT', 'C',  0.00,   '2026-01-01'),
    (2026, 'ABATEMENT', 'M0', 300.00, '2026-01-01'),
    (2026, 'ABATEMENT', 'M1', 400.00, '2026-01-01'),
    (2026, 'ABATEMENT', 'M2', 500.00, '2026-01-01'),
    (2026, 'ABATEMENT', 'M3', 600.00, '2026-01-01'),
    (2026, 'ABATEMENT', 'M4', 700.00, '2026-01-01');

-- Professional expenses abatement: 10% of (gross - cnss), capped at 2000 DT/year.
INSERT INTO tax_configuration (tax_year, tax_type, tax_rate, tax_credit_amount, effective_date) VALUES
    (2026, 'ABATEMENT_PRO', 10.00, 2000.00, '2026-01-01');
