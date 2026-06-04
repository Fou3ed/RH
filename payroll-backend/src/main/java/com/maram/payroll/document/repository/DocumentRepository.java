package com.maram.payroll.document.repository;

import com.maram.payroll.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
