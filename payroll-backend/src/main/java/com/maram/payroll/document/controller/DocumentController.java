package com.maram.payroll.document.controller;

import com.maram.payroll.document.dto.DocumentDto;
import com.maram.payroll.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Employee document upload/list/download/delete. Bytes are stored in MinIO;
 * metadata is returned as {@link DocumentDto}.
 */
@RestController
@Tag(name = "Documents", description = "Employee document storage")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/employees/{employeeId}/documents")
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "List an employee's documents")
    public List<DocumentDto> list(@PathVariable Long employeeId) {
        return documentService.list(employeeId);
    }

    @PostMapping(value = "/employees/{employeeId}/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('employee.edit')")
    @Operation(summary = "Upload a document for an employee (PDF/JPEG/PNG/DOC/DOCX, max 10MB)")
    public ResponseEntity<DocumentDto> upload(
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "description", required = false) String description) {
        DocumentDto dto = documentService.upload(employeeId, type, description, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAuthority('employee.view')")
    @Operation(summary = "Download a document")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        DocumentService.DownloadResource resource = documentService.download(id);
        MediaType mediaType = resource.contentType() != null
                ? MediaType.parseMediaType(resource.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.fileName() + "\"");
        if (resource.size() != null) {
            builder.contentLength(resource.size());
        }
        return builder.body(new InputStreamResource(resource.stream()));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('employee.edit')")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
}
