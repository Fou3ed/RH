package com.maram.payroll.employee.entity;

import com.maram.payroll.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Organisational unit. {@code parent} models the (optional) department hierarchy;
 * the DB column is {@code manager_id} per the original schema.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Department parent;
}
