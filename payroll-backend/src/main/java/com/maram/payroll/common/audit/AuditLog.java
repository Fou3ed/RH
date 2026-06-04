package com.maram.payroll.common.audit;

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
 * Immutable record of a business action (CREATE/UPDATE/DELETE/...) on an entity.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String action;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "change_description")
    private String changeDescription;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "action_timestamp")
    private LocalDateTime actionTimestamp;
}
