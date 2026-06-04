package com.maram.payroll.document.dto;

import com.maram.payroll.document.entity.Document;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDto(
        Long id,
        Long employeeId,
        String documentType,
        String fileName,
        String contentType,
        Long fileSize,
        String description,
        String uploadedBy,
        LocalDateTime createdAt) {

    public static DocumentDto from(Document d) {
        return new DocumentDto(
                d.getId(), d.getEmployeeId(), d.getDocumentType(), d.getFileName(),
                d.getContentType(), d.getFileSize(), d.getDescription(),
                d.getUploadedBy(), d.getCreatedAt());
    }

    public static List<DocumentDto> from(List<Document> docs) {
        return docs.stream().map(DocumentDto::from).toList();
    }
}
