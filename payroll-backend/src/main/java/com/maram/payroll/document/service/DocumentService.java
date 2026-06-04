package com.maram.payroll.document.service;

import com.maram.payroll.auth.security.SecurityUtils;
import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.document.dto.DocumentDto;
import com.maram.payroll.document.entity.Document;
import com.maram.payroll.document.repository.DocumentRepository;
import com.maram.payroll.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Employee document management: validate + store bytes in MinIO, index metadata
 * in the {@code documents} table. (Anti-virus scanning is out of scope for this
 * stack; it would belong in front of {@link #upload} as a pre-store gate.)
 */
@Service
public class DocumentService {

    static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Set<String> ALLOWED_TYPES = Set.of("CONTRACT", "CERTIFICATION", "ID", "OTHER");

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final MinioStorageService storage;
    private final AuditService auditService;

    public DocumentService(DocumentRepository documentRepository,
                           EmployeeRepository employeeRepository,
                           MinioStorageService storage,
                           AuditService auditService) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.storage = storage;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> list(Long employeeId) {
        requireEmployee(employeeId);
        return DocumentDto.from(documentRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId));
    }

    @Transactional
    public DocumentDto upload(Long employeeId, String type, String description, MultipartFile file) {
        requireEmployee(employeeId);
        String documentType = normaliseType(type);
        validateFile(file);

        String objectKey = "employee-documents/%d/%s/%s_%s".formatted(
                employeeId, documentType, UUID.randomUUID(), sanitize(file.getOriginalFilename()));

        try (InputStream in = file.getInputStream()) {
            storage.put(objectKey, in, file.getSize(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file: " + e.getMessage());
        }

        Document doc = new Document();
        doc.setEmployeeId(employeeId);
        doc.setDocumentType(documentType);
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setObjectKey(objectKey);
        doc.setDescription(description);
        doc.setUploadedBy(SecurityUtils.getCurrentUsername().orElse("system"));
        doc.setCreatedAt(LocalDateTime.now());

        Document saved = documentRepository.save(doc);
        auditService.log("DOCUMENT", saved.getId(), "UPLOAD",
                "Uploaded %s for employee %d".formatted(saved.getFileName(), employeeId));
        return DocumentDto.from(saved);
    }

    @Transactional
    public DownloadResource download(Long documentId) {
        Document doc = find(documentId);
        InputStream stream = storage.get(doc.getObjectKey());
        auditService.log("DOCUMENT", documentId, "DOWNLOAD", "Downloaded " + doc.getFileName());
        return new DownloadResource(doc.getFileName(), doc.getContentType(), doc.getFileSize(), stream);
    }

    @Transactional
    public void delete(Long documentId) {
        Document doc = find(documentId);
        storage.remove(doc.getObjectKey());
        documentRepository.delete(doc);
        auditService.log("DOCUMENT", documentId, "DELETE", "Deleted " + doc.getFileName());
    }

    // ---- helpers ----

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType
                    + " (allowed: PDF, JPEG, PNG, DOC, DOCX)");
        }
    }

    private String normaliseType(String type) {
        String normalised = (type == null ? "OTHER" : type.trim().toUpperCase(Locale.ROOT));
        return ALLOWED_TYPES.contains(normalised) ? normalised : "OTHER";
    }

    private void requireEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
    }

    private Document find(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** Streamed download payload. */
    public record DownloadResource(String fileName, String contentType, Long size, InputStream stream) {
    }
}
