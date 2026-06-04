-- ============================================================
-- V9 — Employee documents (metadata; bytes live in MinIO)
-- ============================================================

CREATE TABLE documents (
    id            BIGSERIAL PRIMARY KEY,
    employee_id   BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,   -- CONTRACT, CERTIFICATION, ID, OTHER
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100),
    file_size     BIGINT,
    object_key    VARCHAR(500) NOT NULL,  -- MinIO object path
    description   VARCHAR(500),
    uploaded_by   VARCHAR(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_documents_employee ON documents (employee_id);
CREATE INDEX idx_documents_type ON documents (document_type);
