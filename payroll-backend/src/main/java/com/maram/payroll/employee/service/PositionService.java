package com.maram.payroll.employee.service;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.employee.dto.PositionDto;
import com.maram.payroll.employee.dto.PositionRequest;
import com.maram.payroll.employee.entity.Department;
import com.maram.payroll.employee.entity.Position;
import com.maram.payroll.employee.mapper.PositionMapper;
import com.maram.payroll.employee.repository.DepartmentRepository;
import com.maram.payroll.employee.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PositionService {

    private final PositionRepository repository;
    private final DepartmentRepository departmentRepository;
    private final PositionMapper mapper;
    private final AuditService auditService;

    public PositionService(PositionRepository repository,
                           DepartmentRepository departmentRepository,
                           PositionMapper mapper,
                           AuditService auditService) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PositionDto> list(Long departmentId) {
        List<Position> positions = (departmentId == null)
                ? repository.findAll()
                : repository.findByDepartmentId(departmentId);
        return positions.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PositionDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public PositionDto create(PositionRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new ConflictException("Position code already exists: " + request.code());
        }
        Position position = new Position();
        apply(position, request);
        Position saved = repository.save(position);
        auditService.log("POSITION", saved.getId(), "CREATE", "Created position " + saved.getCode());
        return mapper.toDto(saved);
    }

    @Transactional
    public PositionDto update(Long id, PositionRequest request) {
        Position position = find(id);
        if (!position.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new ConflictException("Position code already exists: " + request.code());
        }
        apply(position, request);
        auditService.log("POSITION", position.getId(), "UPDATE", "Updated position " + position.getCode());
        return mapper.toDto(position);
    }

    @Transactional
    public void delete(Long id) {
        Position position = find(id);
        repository.delete(position);
        auditService.log("POSITION", id, "DELETE", "Deleted position " + position.getCode());
    }

    private void apply(Position position, PositionRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentId()));
        position.setCode(request.code());
        position.setName(request.name());
        position.setDescription(request.description());
        position.setDepartment(department);
        position.setSalaryGrade(request.salaryGrade());
    }

    private Position find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position", id));
    }
}
