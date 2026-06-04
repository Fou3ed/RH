-- ============================================================
-- V5 — Authentication, RBAC & audit trail
-- (Parent tables created before the tables that reference them.)
-- ============================================================

-- Roles
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,  -- ADMIN, HR_MANAGER, FINANCE_MANAGER, MANAGER, EMPLOYEE
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Permissions
CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) UNIQUE NOT NULL, -- e.g. employee.view, payroll.approve
    name        VARCHAR(255),
    description TEXT,
    resource    VARCHAR(100),
    action      VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users (application accounts; optionally linked to an employee)
CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    username              VARCHAR(100) UNIQUE NOT NULL,
    email                 VARCHAR(100) UNIQUE NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    employee_id           BIGINT REFERENCES employees (id),
    status                VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, LOCKED
    failed_login_attempts INT DEFAULT 0,
    last_login_at         TIMESTAMP,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_employee ON users (employee_id);

-- User ↔ Role
CREATE TABLE user_roles (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

-- Role ↔ Permission
CREATE TABLE role_permissions (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions (role_id);

-- Audit log
CREATE TABLE audit_log (
    id                 BIGSERIAL PRIMARY KEY,
    entity_type        VARCHAR(50) NOT NULL,  -- EMPLOYEE, PAYROLL, ATTENDANCE, ...
    entity_id          BIGINT NOT NULL,
    action             VARCHAR(50) NOT NULL,  -- CREATE, UPDATE, DELETE, APPROVE, REJECT
    old_value          TEXT,                  -- JSON
    new_value          TEXT,                  -- JSON
    change_description VARCHAR(500),
    user_id            VARCHAR(100),
    user_name          VARCHAR(100),
    action_timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address         VARCHAR(50),
    user_agent         VARCHAR(255)
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_log (action);
CREATE INDEX idx_audit_timestamp ON audit_log (action_timestamp);
CREATE INDEX idx_audit_user ON audit_log (user_id);
