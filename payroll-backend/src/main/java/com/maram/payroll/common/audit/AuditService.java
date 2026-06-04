package com.maram.payroll.common.audit;

import com.maram.payroll.auth.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Records business actions to the {@code audit_log} table. Intentionally simple
 * for Sprint 3 — captures entity, action, actor and a human-readable description.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String entityType, Long entityId, String action, String description) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setChangeDescription(description);
        entry.setUserName(SecurityUtils.getCurrentUsername().orElse("system"));
        entry.setUserId(SecurityUtils.getCurrentUserId().map(String::valueOf).orElse(null));
        entry.setActionTimestamp(LocalDateTime.now());
        repository.save(entry);
    }
}
