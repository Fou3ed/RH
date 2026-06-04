package com.maram.payroll.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Metadata for a stored employee document. The bytes live in MinIO under
 * {@link #objectKey}; this row is the index entry.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    private String description;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
