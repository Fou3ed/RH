-- ============================================================
-- V8 — Add audit-author columns to positions for consistency with
-- departments/employees (so the Auditable mapped-superclass applies uniformly).
-- ============================================================

ALTER TABLE positions ADD COLUMN created_by VARCHAR(100);
ALTER TABLE positions ADD COLUMN updated_by VARCHAR(100);
