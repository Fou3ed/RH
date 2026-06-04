package com.maram.payroll.employee.service;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.employee.dto.DepartmentDto;
import com.maram.payroll.employee.dto.DepartmentRequest;
import com.maram.payroll.employee.entity.Department;
import com.maram.payroll.employee.mapper.DepartmentMapper;
import com.maram.payroll.employee.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository repository;
    private final DepartmentMapper mapper;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository repository,
                             DepartmentMapper mapper,
                             AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public DepartmentDto create(DepartmentRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new ConflictException("Department code already exists: " + request.code());
        }
        Department department = new Department();
        apply(department, request);
        Department saved = repository.save(department);
        auditService.log("DEPARTMENT", saved.getId(), "CREATE", "Created department " + saved.getCode());
        return mapper.toDto(saved);
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentRequest request) {
        Department department = find(id);
        if (!department.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new ConflictException("Department code already exists: " + request.code());
        }
        apply(department, request);
        auditService.log("DEPARTMENT", department.getId(), "UPDATE", "Updated department " + department.getCode());
        return mapper.toDto(department);
    }

    @Transactional
    public void delete(Long id) {
        Department department = find(id);
        repository.delete(department);
        auditService.log("DEPARTMENT", id, "DELETE", "Deleted department " + department.getCode());
    }

    private void apply(Department department, DepartmentRequest request) {
        department.setCode(request.code());
        department.setName(request.name());
        department.setDescription(request.description());
        department.setParent(resolveParent(department, request.parentId()));
    }

    private Department resolveParent(Department department, Long parentId) {
        if (parentId == null) {
            return null;
        }
        if (department.getId() != null && department.getId().equals(parentId)) {
            throw new IllegalArgumentException("A department cannot be its own parent");
        }
        return repository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", parentId));
    }

    private Department find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }
}
