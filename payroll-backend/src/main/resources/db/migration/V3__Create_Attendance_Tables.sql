-- ============================================================
-- V3 — Daily attendance records
-- ============================================================

CREATE TABLE attendance (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT NOT NULL REFERENCES employees (id),
    attendance_date   DATE NOT NULL,
    day_of_week       VARCHAR(15),
    attendance_code   VARCHAR(5),               -- 8, 7, 4, A, ...
    attendance_status VARCHAR(20) NOT NULL,     -- PRESENT, ABSENT, HALF_DAY, LEAVE, HOLIDAY, WEEKEND
    days_fraction     DECIMAL(3, 2) DEFAULT 1.0,
    clock_in_time     TIME,
    clock_out_time    TIME,
    hours_worked      DECIMAL(5, 2),
    notes             TEXT,
    absence_reason    VARCHAR(100),
    is_paid_leave     BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(100),
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, attendance_date)
);

CREATE INDEX idx_attendance_employee ON attendance (employee_id);
CREATE INDEX idx_attendance_date ON attendance (attendance_date);
CREATE INDEX idx_attendance_status ON attendance (attendance_status);
