-- ============================================================
-- V6 — Reference / seed data
--   * salary categories          * RBAC roles & permissions
--   * a default department        * 2026 tax configuration (IRPP/CNSS/HEALTH)
-- The default ADMIN *user account* is created at startup by DataInitializer
-- (so the password is hashed with the application's BCrypt encoder).
-- ============================================================

-- ---- Salary categories ----
INSERT INTO salary_categories (code, name, base_multiplier) VALUES
    ('CAT1', 'Category 1', 1.000),
    ('CAT3', 'Category 3', 1.000);

-- ---- Default org structure (placeholder until real data is imported) ----
INSERT INTO departments (code, name, description) VALUES
    ('GEN', 'General', 'Default department');

-- ---- Roles ----
INSERT INTO roles (code, name, description) VALUES
    ('ADMIN',           'Administrator',    'Full system access'),
    ('HR_MANAGER',      'HR Manager',       'Employee and attendance management'),
    ('FINANCE_MANAGER', 'Finance Manager',  'Payroll approval and payments'),
    ('MANAGER',         'Manager',          'Performance ratings and team attendance'),
    ('EMPLOYEE',        'Employee',         'View own data and download payslips');

-- ---- Permissions ----
INSERT INTO permissions (code, name, resource, action) VALUES
    ('employee.view',     'View Employee',       'employee',    'view'),
    ('employee.create',   'Create Employee',     'employee',    'create'),
    ('employee.edit',     'Edit Employee',       'employee',    'edit'),
    ('employee.delete',   'Delete Employee',     'employee',    'delete'),
    ('employee.import',   'Import Employees',    'employee',    'import'),
    ('attendance.view',   'View Attendance',     'attendance',  'view'),
    ('attendance.record', 'Record Attendance',   'attendance',  'record'),
    ('attendance.import', 'Import Attendance',   'attendance',  'import'),
    ('payroll.view',      'View Payroll',        'payroll',     'view'),
    ('payroll.calculate', 'Calculate Payroll',   'payroll',     'calculate'),
    ('payroll.approve',   'Approve Payroll',     'payroll',     'approve'),
    ('payroll.export',    'Export Payroll',      'payroll',     'export'),
    ('performance.view',  'View Performance',    'performance', 'view'),
    ('performance.rate',  'Rate Performance',    'performance', 'rate'),
    ('report.view',       'View Reports',        'report',      'view'),
    ('report.export',     'Export Reports',      'report',      'export'),
    ('config.manage',     'Manage Configuration','config',      'manage'),
    ('user.manage',       'Manage Users',        'user',        'manage'),
    ('role.manage',       'Manage Roles',        'role',        'manage'),
    ('audit.view',        'View Audit Log',      'audit',       'view');

-- ---- Role ⇄ Permission mappings ----
-- ADMIN: everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- HR_MANAGER: employees, attendance, performance, reports, payroll view/calculate
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'employee.view','employee.create','employee.edit','employee.import',
    'attendance.view','attendance.record','attendance.import',
    'performance.view','performance.rate',
    'payroll.view','payroll.calculate',
    'report.view','report.export'
) WHERE r.code = 'HR_MANAGER';

-- FINANCE_MANAGER: payroll lifecycle + reports
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'payroll.view','payroll.calculate','payroll.approve','payroll.export',
    'employee.view','report.view','report.export'
) WHERE r.code = 'FINANCE_MANAGER';

-- MANAGER: team view + performance ratings
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'employee.view','attendance.view','attendance.record',
    'performance.view','performance.rate','report.view'
) WHERE r.code = 'MANAGER';

-- EMPLOYEE: self-service read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'payroll.view','attendance.view','performance.view'
) WHERE r.code = 'EMPLOYEE';

-- ---- Tax configuration (2026, illustrative brackets — confirm with finance) ----
-- IRPP progressive brackets
INSERT INTO tax_configuration (tax_year, tax_type, min_taxable_income, max_taxable_income, tax_rate, effective_date) VALUES
    (2026, 'IRPP',     0.00,     2000.00,  0.00,  '2026-01-01'),
    (2026, 'IRPP',  2000.00,     5000.00, 10.00,  '2026-01-01'),
    (2026, 'IRPP',  5000.00,    10000.00, 20.00,  '2026-01-01'),
    (2026, 'IRPP', 10000.00, 99999999.00, 30.00,  '2026-01-01');

-- CNSS employee contribution rate (% of gross)
INSERT INTO tax_configuration (tax_year, tax_type, tax_rate, effective_date) VALUES
    (2026, 'CNSS',   5.95, '2026-01-01'),
    (2026, 'HEALTH', 0.00, '2026-01-01');
