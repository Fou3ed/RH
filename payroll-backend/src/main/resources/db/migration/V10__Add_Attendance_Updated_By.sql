-- ============================================================
-- V10 — Add updated_by to attendance so the Auditable superclass applies
-- (consistent with departments/positions/employees).
-- ============================================================

ALTER TABLE attendance ADD COLUMN updated_by VARCHAR(100);
